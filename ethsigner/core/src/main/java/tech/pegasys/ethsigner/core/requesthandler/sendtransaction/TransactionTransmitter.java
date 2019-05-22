/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static java.util.Collections.singletonList;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionTransmitter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  private final HttpClient ethNodeClient;
  private final TransactionSerialiser transactionSerialiser;
  private final SendTransactionContext sendTransactionContext;
  private final RetryMechanism<SendTransactionContext> retryMechanism;
  private final HttpResponseFactory responder;
  private final VertxRequestTransmitter transmitter;

  public TransactionTransmitter(
      final HttpClient ethNodeClient,
      final SendTransactionContext sendTransactionContext,
      final TransactionSerialiser transactionSerialiser,
      final RetryMechanism<SendTransactionContext> retryMechanism,
      final HttpResponseFactory responder,
      final VertxRequestTransmitterFactory vertxTransmitterFactory) {
    this.transmitter = vertxTransmitterFactory.create(this::handleResponseBody);
    this.ethNodeClient = ethNodeClient;
    this.sendTransactionContext = sendTransactionContext;
    this.transactionSerialiser = transactionSerialiser;
    this.retryMechanism = retryMechanism;
    this.responder = responder;
  }

  public void send() {
    createSignedTransactionBody();
  }

  private void createSignedTransactionBody() {
    // This assumes the parameters have already been validated and are correct for the unlocked
    // account.
    final String signedTransactionHexString;
    try {
      sendTransactionContext.getPreTransmitOperation().run();
      final Transaction transaction = sendTransactionContext.getTransaction();
      signedTransactionHexString = transactionSerialiser.serialise(transaction);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Failed to encode transaction: {}", sendTransactionContext.getTransaction(), e);
      sendTransactionContext
          .getRoutingContext()
          .fail(BAD_REQUEST.code(), new JsonRpcException(JsonRpcError.INVALID_PARAMS));
      return;
    } catch (final RuntimeException e) {
      LOG.info("Unable to get nonce from web3j provider.");
      final Throwable cause = e.getCause();
      if (cause instanceof SocketException || cause instanceof SocketTimeoutException) {
        sendTransactionContext
            .getRoutingContext()
            .fail(
                GATEWAY_TIMEOUT.code(),
                new JsonRpcException(CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT));
      } else {
        sendTransactionContext
            .getRoutingContext()
            .fail(GATEWAY_TIMEOUT.code(), new JsonRpcException(INTERNAL_ERROR));
      }
      return;
    } catch (final Throwable thrown) {
      LOG.debug(
          "Failed to encode/serialise transaction: {}",
          sendTransactionContext.getTransaction(),
          thrown);
      sendTransactionContext
          .getRoutingContext()
          .fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
      return;
    }

    final JsonRpcRequest sendRawTransaction = new JsonRpcRequest(JSON_RPC_VERSION, JSON_RPC_METHOD);
    sendRawTransaction.setParams(singletonList(signedTransactionHexString));
    sendRawTransaction.setId(sendTransactionContext.getId());

    try {
      sendTransaction(Json.encodeToBuffer(sendRawTransaction));
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Serialisation failed for: {}", sendRawTransaction, e);
      sendTransactionContext
          .getRoutingContext()
          .fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
    }
  }

  private void sendTransaction(final Buffer bodyContent) {
    final HttpServerRequest httpServerRequest = sendTransactionContext.getInitialRequest();
    final HttpClientRequest request =
        ethNodeClient.request(
            httpServerRequest.method(),
            httpServerRequest.uri(),
            response ->
                transmitter.handleResponse(sendTransactionContext.getRoutingContext(), response));

    transmitter.sendRequest(request, bodyContent, sendTransactionContext.getRoutingContext());
  }

  private void handleResponseBody(
      final RoutingContext context, final HttpClientResponse response, final Buffer body) {
    if (response.statusCode() != HttpResponseStatus.OK.code()
        && retryMechanism.responseRequiresRetry(response, body)) {
      if (retryMechanism.retriesAvailable()) {
        retryMechanism.incrementRetries();
        send();
        return;
      } else {
        reportError(); // This needs to become a context.fail.
        return;
      }
    }

    final HttpServerRequest httpServerRequest = context.request();
    httpServerRequest.response().setStatusCode(response.statusCode());
    httpServerRequest.response().headers().setAll(response.headers());
    httpServerRequest.response().setChunked(false);
    httpServerRequest.response().end(body);
  }

  private void reportError() {
    final JsonRpcErrorResponse errorResponse =
        new JsonRpcErrorResponse(sendTransactionContext.getId(), INTERNAL_ERROR);

    LOG.debug(
        "Dropping request method: {}, uri: {}, body: {}, Error body: {}",
        sendTransactionContext.getInitialRequest()::method,
        sendTransactionContext.getInitialRequest()::absoluteURI,
        sendTransactionContext::getTransaction,
        () -> Json.encode(errorResponse));

    responder.create(
        sendTransactionContext.getInitialRequest(),
        HttpResponseStatus.BAD_REQUEST.code(),
        errorResponse);
  }
}

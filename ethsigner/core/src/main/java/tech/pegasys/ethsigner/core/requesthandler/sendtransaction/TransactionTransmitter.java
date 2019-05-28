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
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;

import java.net.SocketException;
import java.net.SocketTimeoutException;

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

  private final HttpClient ethNodeClient;
  private final TransactionSerialiser transactionSerialiser;
  private final SendTransactionContext sendTransactionContext;
  private final VertxRequestTransmitter transmitter;
  private final NonceProvider nonceProvider;

  public TransactionTransmitter(
      final HttpClient ethNodeClient,
      final SendTransactionContext sendTransactionContext,
      final TransactionSerialiser transactionSerialiser,
      final VertxRequestTransmitterFactory vertxTransmitterFactory,
      final NonceProvider nonceProvider) {

    transmitter = vertxTransmitterFactory.create(this::handleResponseBody);
    this.ethNodeClient = ethNodeClient;
    this.sendTransactionContext = sendTransactionContext;
    this.transactionSerialiser = transactionSerialiser;

    this.nonceProvider = nonceProvider;
  }

  public void send() {
    createSignedTransactionBody();
  }

  private void createSignedTransactionBody() {
    final String signedTransactionHexString;
    try {
      final Transaction transaction = sendTransactionContext.getTransaction();
      if (!transaction.isNonceUserSpecified()) {
        transaction.updateNonce(nonceProvider.getNonce());
      }

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

    final JsonRpcRequest rawTransaction =
        sendTransactionContext
            .getTransaction()
            .jsonRpcRequest(signedTransactionHexString, sendTransactionContext.getId());
    try {
      sendTransaction(Json.encodeToBuffer(rawTransaction));
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Serialisation failed for: {}", rawTransaction, e);
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

  protected void handleResponseBody(
      final RoutingContext context, final HttpClientResponse response, final Buffer body) {
    final HttpServerRequest httpServerRequest = context.request();
    httpServerRequest.response().setStatusCode(response.statusCode());
    httpServerRequest.response().headers().setAll(response.headers());
    httpServerRequest.response().setChunked(false);
    httpServerRequest.response().end(body);
  }
}

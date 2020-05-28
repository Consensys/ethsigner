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

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
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
import tech.pegasys.ethsigner.core.signing.TransactionSerializer;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionTransmitter {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpClient ethNodeClient;
  private final TransactionSerializer transactionSerializer;
  private final Transaction transaction;
  private final VertxRequestTransmitter transmitter;
  private final RoutingContext routingContext;
  private final String downstreamPath;

  public TransactionTransmitter(
      final HttpClient ethNodeClient,
      final String downstreamPath,
      final Transaction transaction,
      final TransactionSerializer transactionSerializer,
      final VertxRequestTransmitterFactory vertxTransmitterFactory,
      final RoutingContext routingContext) {
    this.transmitter = vertxTransmitterFactory.create(this::handleResponseBody);
    this.ethNodeClient = ethNodeClient;
    this.downstreamPath = downstreamPath;
    this.transaction = transaction;
    this.transactionSerializer = transactionSerializer;
    this.routingContext = routingContext;
  }

  public void send() {
    final Optional<JsonRpcRequest> request = createSignedTransactionBody();

    if (request.isEmpty()) {
      return;
    }

    try {
      sendTransaction(Json.encodeToBuffer(request.get()));
    } catch (final IllegalArgumentException | EncodeException e) {
      LOG.debug("JSON Serialization failed for: {}", request, e);
      routingContext.fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
    }
  }

  private Optional<JsonRpcRequest> createSignedTransactionBody() {

    if (!transaction.isNonceUserSpecified()) {
      if (!populateNonce()) {
        return Optional.empty();
      }
    }

    final String signedTransactionHexString;
    try {
      signedTransactionHexString = transactionSerializer.serialize(transaction);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Failed to encode transaction: {}", transaction, e);
      routingContext.fail(BAD_REQUEST.code(), new JsonRpcException(JsonRpcError.INVALID_PARAMS));
      return Optional.empty();
    } catch (final Throwable thrown) {
      LOG.debug("Failed to encode transaction: {}", transaction, thrown);
      routingContext.fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
      return Optional.empty();
    }

    return Optional.of(transaction.jsonRpcRequest(signedTransactionHexString, transaction.getId()));
  }

  private boolean populateNonce() {
    try {
      transaction.updateNonce();
      return true;
    } catch (final RuntimeException e) {
      LOG.warn("Unable to get nonce from web3j provider.", e);
      final Throwable cause = e.getCause();
      if (cause instanceof SocketException
          || cause instanceof SocketTimeoutException
          || cause instanceof TimeoutException) {
        routingContext.fail(
            GATEWAY_TIMEOUT.code(), new JsonRpcException(CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT));
      } else if (cause instanceof SSLHandshakeException) {
        routingContext.fail(BAD_GATEWAY.code(), cause);
      } else {
        routingContext.fail(GATEWAY_TIMEOUT.code(), new JsonRpcException(INTERNAL_ERROR));
      }
    } catch (final Throwable thrown) {
      LOG.debug("Failed to encode/serialize transaction: {}", transaction, thrown);
      routingContext.fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
    }
    return false;
  }

  private void sendTransaction(final Buffer bodyContent) {
    final HttpClientRequest request =
        ethNodeClient.post(
            downstreamPath, response -> transmitter.handleResponse(routingContext, response));

    transmitter.sendRequest(request, bodyContent, routingContext);
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

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
package tech.pegasys.ethsigner.requesthandler.sendtransaction;

import static java.util.Collections.singletonList;

import tech.pegasys.ethsigner.http.HttpResponseFactory;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.signing.TransactionSerialiser;

import java.io.IOException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class TransactionTransmitter {

  private static final Logger LOG = LoggerFactory.getLogger(TransactionTransmitter.class);
  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  private final HttpClient ethNodeClient;
  private final TransactionSerialiser serialiser;
  private final SendTransactionContext context;
  private final RetryMechanism retryMechanism;
  private final HttpResponseFactory responder;

  public TransactionTransmitter(
      final HttpClient ethNodeClient,
      final SendTransactionContext context,
      final TransactionSerialiser serialiser,
      final RetryMechanism retryMechanism,
      HttpResponseFactory responder) {
    this.ethNodeClient = ethNodeClient;
    this.context = context;
    this.serialiser = serialiser;
    this.retryMechanism = retryMechanism;
    this.responder = responder;
  }

  public void send() {
    final JsonRpcBody body = getBody();
    if (body.hasError()) {
      reportError(JsonRpcError.INTERNAL_ERROR);
    } else {
      sendTransaction(body.body());
    }
  }

  private JsonRpcBody getBody() {
    // This assumes the parameters have already been validated and are correct for the unlocked
    // account.
    final String signedTransactionHexString;
    try {
      final RawTransaction rawTransaction = context.getRawTransactionBuilder().build();
      signedTransactionHexString = serialiser.serialise(rawTransaction);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Failed to encode transaction: {}", context.getRawTransactionBuilder(), e);
      return new JsonRpcBody(JsonRpcError.INVALID_PARAMS);
    } catch (final Throwable e) {
      LOG.debug(
          "Failed to encode/serialise transaction: {}", context.getRawTransactionBuilder(), e);
      return new JsonRpcBody(JsonRpcError.INTERNAL_ERROR);
    }

    final JsonRpcRequest sendRawTransaction =
        new JsonRpcRequest(
            JSON_RPC_VERSION, JSON_RPC_METHOD, singletonList(signedTransactionHexString));
    sendRawTransaction.setId(context.getId());

    try {
      return new JsonRpcBody(Json.encodeToBuffer(sendRawTransaction));
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Serialisation failed for: {}", sendRawTransaction, e);
      return new JsonRpcBody(JsonRpcError.INTERNAL_ERROR);
    }
  }

  private void sendTransaction(final Buffer bodyContent) {
    final HttpServerRequest httpServerRequest = context.getInitialRequest();
    final HttpClientRequest proxyRequest =
        ethNodeClient.request(
            httpServerRequest.method(), httpServerRequest.uri(), this::handleResponse);

    proxyRequest.headers().setAll(httpServerRequest.headers());
    proxyRequest.headers().remove("Content-Length"); // created during 'end'.
    proxyRequest.setChunked(false);
    proxyRequest.end(bodyContent);
  }

  private void handleResponse(final HttpClientResponse response) {
    logResponse(response);
    response.bodyHandler(
        body -> {
          logResponseBody(body);
          handleResponseBody(response, body);
        });
  }

  private void handleResponseBody(final HttpClientResponse response, final Buffer body) {
    try {
      if (response.statusCode() != HttpResponseStatus.OK.code()
          && retryMechanism.mustRetry(response, body)) {
        send();
        return;
      }
    } catch (final IOException e) {
      LOG.info("Retry mechanism failed, reporting error.");
      reportError(JsonRpcError.INTERNAL_ERROR);
      return;
    }

    final HttpServerRequest httpServerRequest = context.getInitialRequest();
    httpServerRequest.response().setStatusCode(response.statusCode());
    httpServerRequest.response().headers().setAll(response.headers());
    httpServerRequest.response().setChunked(false);
    httpServerRequest.response().end(body);
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  protected void reportError(final JsonRpcError errorCode) {
    final JsonRpcErrorResponse errorResponse = new JsonRpcErrorResponse(context.getId(), errorCode);

    LOG.debug(
        "Dropping request method: {}, uri: {}, body: {}, Error body: {}",
        context.getInitialRequest().method(),
        context.getInitialRequest().absoluteURI(),
        context.getRawTransactionBuilder().toString(),
        Json.encode(errorResponse));

    responder.create(
        context.getInitialRequest(), HttpResponseStatus.BAD_REQUEST.code(), errorResponse);
  }
}

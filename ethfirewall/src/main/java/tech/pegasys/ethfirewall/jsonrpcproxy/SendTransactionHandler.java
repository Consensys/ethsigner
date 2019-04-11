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
package tech.pegasys.ethfirewall.jsonrpcproxy;

import static java.util.Collections.singletonList;

import tech.pegasys.ethfirewall.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethfirewall.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethfirewall.jsonrpc.SendTransactionJsonParameters;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendTransactionHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SendTransactionHandler.class);

  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  private final JsonRpcErrorReporter errorReporter;
  private final HttpClient ethNodeClient;
  private final TransactionSigner signer;

  public SendTransactionHandler(
      final JsonRpcErrorReporter errorReporter,
      final HttpClient ethNodeClient,
      TransactionSigner signer) {
    this.errorReporter = errorReporter;
    this.ethNodeClient = ethNodeClient;
    this.signer = signer;
  }

  @Override
  public void handle(final HttpServerRequest httpServerRequest, final JsonRpcRequest requestBody) {
    final HttpClientRequest request =
        ethNodeClient.request(
            httpServerRequest.method(),
            httpServerRequest.uri(),
            proxiedResponse -> {
              logResponse(proxiedResponse);

              httpServerRequest.response().setStatusCode(proxiedResponse.statusCode());
              httpServerRequest.response().headers().setAll(proxiedResponse.headers());
              httpServerRequest.response().setChunked(false);

              proxiedResponse.bodyHandler(
                  data -> {
                    logResponseBody(data);

                    // End the sendRequest, preventing any other handler from executing
                    httpServerRequest.response().end(data);
                  });
            });

    request.headers().setAll(httpServerRequest.headers());
    request.headers().remove("Content-Length"); // created during 'end'.
    request.setChunked(false);

    final JsonRpcBody rpcBody = getBody(requestBody);
    if (rpcBody.hasError()) {
      errorReporter.send(requestBody, httpServerRequest, rpcBody.error());
    } else {
      logRequest(requestBody, httpServerRequest, request, rpcBody.body());
      request.end(rpcBody.body());
    }
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  private void logRequest(
      final JsonRpcRequest originalJsonRpcRequest,
      final HttpServerRequest originalRequest,
      final HttpClientRequest proxyRequest,
      final Buffer proxyRequestBody) {
    LOG.debug(
        "Original method: {}, uri: {}, body: {}, Proxy: method: {}, uri: {}, body: {}",
        originalRequest.method(),
        originalRequest.absoluteURI(),
        Json.encodePrettily(originalJsonRpcRequest),
        proxyRequest.method(),
        proxyRequest.absoluteURI(),
        proxyRequestBody);
  }

  private JsonRpcBody getBody(final JsonRpcRequest request) {

    final SendTransactionJsonParameters params;
    try {
      params = SendTransactionJsonParameters.from(request);
    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      return createJsonRpcBodyFrom(request.getId(), JsonRpcError.INVALID_PARAMS);
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Deserialisation failed for request: {}", request.getParams(), e);
      return createJsonRpcBodyFrom(request.getId(), JsonRpcError.INVALID_PARAMS);
    }

    final String signedTransactionHexString;
    try {
      signedTransactionHexString = signer.signTransaction(params);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Bad input value from request: {}", request, e);
      return createJsonRpcBodyFrom(request.getId(), JsonRpcError.INVALID_PARAMS);
    } catch (final Throwable e) {
      LOG.debug("Unhandled error processing request: {}", request, e);
      return createJsonRpcBodyFrom(request.getId(), JsonRpcError.INTERNAL_ERROR);
    }

    final JsonRpcRequest sendRawTransaction =
        new JsonRpcRequest(
            JSON_RPC_VERSION, JSON_RPC_METHOD, singletonList(signedTransactionHexString));
    sendRawTransaction.setId(request.getId());

    try {
      return new JsonRpcBody(Json.encodeToBuffer(sendRawTransaction));
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Serialisation failed for: {}", sendRawTransaction, e);
      return createJsonRpcBodyFrom(request.getId(), JsonRpcError.INTERNAL_ERROR);
    }
  }

  private static JsonRpcBody createJsonRpcBodyFrom(
      final JsonRpcRequestId id, final JsonRpcError error) {
    return new JsonRpcBody(new JsonRpcErrorResponse(id, error));
  }
}

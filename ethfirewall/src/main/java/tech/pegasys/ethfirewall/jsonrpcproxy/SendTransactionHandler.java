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

import tech.pegasys.ethfirewall.jsonrpc.JsonRpcRequest;

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
  private final JsonRpcErrorReporter errorReporter;
  private final HttpClient ethNodeClient;
  private final BodyProvider bodyProvider;

  public SendTransactionHandler(
      final JsonRpcErrorReporter errorReporter,
      final HttpClient ethNodeClient,
      final BodyProvider bodyProvider) {
    this.errorReporter = errorReporter;
    this.ethNodeClient = ethNodeClient;
    this.bodyProvider = bodyProvider;
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

    final JsonRpcBody providedBody = bodyProvider.getBody(requestBody);

    if (providedBody.hasError()) {
      errorReporter.send(requestBody, httpServerRequest, providedBody.error());
    } else {
      // Data is only written to the wire on end()
      final Buffer proxyRequestBody = providedBody.body();
      request.end(proxyRequestBody);
      logRequest(requestBody, httpServerRequest, request, proxyRequestBody);
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
}

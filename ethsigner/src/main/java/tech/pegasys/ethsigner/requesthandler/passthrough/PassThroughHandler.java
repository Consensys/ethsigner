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
package tech.pegasys.ethsigner.requesthandler.passthrough;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.requesthandler.JsonRpcRequestHandler;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassThroughHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PassThroughHandler.class);
  private final HttpClient ethNodeClient;

  public PassThroughHandler(final HttpClient ethNodeClient) {
    this.ethNodeClient = ethNodeClient;
  }

  @Override
  public void handle(final HttpServerRequest httpServerRequest, final JsonRpcRequest request) {
    final HttpClientRequest proxyRequest =
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

    proxyRequest.headers().setAll(httpServerRequest.headers());
    proxyRequest.setChunked(false);

    proxyRequest.end(Json.encodeToBuffer(request));
    logRequest(request, httpServerRequest);
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  private void logRequest(final JsonRpcRequest jsonRequest, final HttpServerRequest httpRequest) {
    LOG.debug(
        "Proxying method: {}, uri: {}, body: {}",
        httpRequest.method(),
        httpRequest.absoluteURI(),
        Json.encodePrettily(jsonRequest));
  }
}

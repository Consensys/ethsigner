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
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
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
  private BodyProvider bodyProvider;

  public PassThroughHandler(final HttpClient ethNodeClient, final BodyProvider bodyProvider) {
    this.ethNodeClient = ethNodeClient;
    this.bodyProvider = bodyProvider;
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
    proxyRequest.headers().remove("Content-Length"); // created during 'end'.
    proxyRequest.setChunked(false);

    final JsonRpcBody providedBody = bodyProvider.getBody(request);

    if (providedBody.hasError()) {
      sendErrorResponse(httpServerRequest, providedBody.error());
    } else {
      // Data is only written to the wire on end()
      final Buffer proxyRequestBody = providedBody.body();
      proxyRequest.end(proxyRequestBody);
      logRequest(httpServerRequest, proxyRequest, proxyRequestBody);
    }
  }

  private void sendErrorResponse(
      final HttpServerRequest httpRequest, final JsonRpcErrorResponse error) {
    LOG.info("Dropping request from {}", httpRequest.remoteAddress());
    /*
    httpRequest.bodyHandler(
        body -> {
          LOG.debug(
              "Dropping request method: {}, uri: {}, body: {}, Error body: {}",
              httpRequest.method(),
              httpRequest.absoluteURI(),
              body.toString(),
              Json.encode(error));
        });
        */

    httpRequest.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    httpRequest.response().headers().setAll(httpRequest.headers());
    httpRequest.response().headers().remove("Content-Length"); // created during 'end'.
    httpRequest.response().setChunked(false);

    httpRequest.response().end(Json.encodeToBuffer(error));
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  private void logRequest(
      final HttpServerRequest originalRequest,
      final HttpClientRequest proxyRequest,
      final Buffer proxyRequestBody) {
    originalRequest.bodyHandler(
        body -> {
          LOG.debug(
              "Original method: {}, uri: {}, body: {}, Proxy: method: {}, uri: {}, body: {}",
              originalRequest.method(),
              originalRequest.absoluteURI(),
              body.toString(),
              proxyRequest.method(),
              proxyRequest.absoluteURI(),
              proxyRequestBody);
        });
  }
}

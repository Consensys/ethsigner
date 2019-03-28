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

import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassThroughHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(PassThroughHandler.class);
  private final HttpClient ethNodeClient;
  private BodyProvider bodyProvider;

  public PassThroughHandler(final HttpClient ethNodeClient, final BodyProvider bodyProvider) {
    this.ethNodeClient = ethNodeClient;
    this.bodyProvider = bodyProvider;
  }

  @Override
  public void handle(final RoutingContext context) {
    final HttpServerRequest originalRequest = context.request();
    final HttpClientRequest proxyRequest =
        ethNodeClient.request(
            originalRequest.method(),
            originalRequest.uri(),
            proxiedResponse -> {
              logResponse(proxiedResponse);

              originalRequest.response().setStatusCode(proxiedResponse.statusCode());
              originalRequest.response().headers().setAll(proxiedResponse.headers());
              originalRequest.response().setChunked(false);

              proxiedResponse.bodyHandler(
                  data -> {
                    logResponseBody(data);

                    // End the sendRequest, preventing any other handler from executing
                    originalRequest.response().end(data);
                  });
            });

    proxyRequest.headers().setAll(originalRequest.headers());
    proxyRequest.headers().remove("Content-Length"); // created during 'end'.
    proxyRequest.setChunked(false);

    final JsonRpcBody providedBody = bodyProvider.getBody(context);

    if (providedBody.hasError()) {
      sendErrorResponse(context, originalRequest, providedBody.error());
    } else {
      // Data is only written to the wire on end()
      final Buffer proxyRequestBody = providedBody.body();
      proxyRequest.end(proxyRequestBody);
      logRequest(context, proxyRequest, proxyRequestBody);
    }
  }

  private void sendErrorResponse(
      final RoutingContext context,
      final HttpServerRequest originalRequest,
      final JsonRpcErrorResponse error) {
    LOG.info("Dropping request from {}", originalRequest.remoteAddress());
    LOG.debug(
        "Dropping request method: {}, uri: {}, body: {}, Error body: {}",
        originalRequest.method(),
        originalRequest.absoluteURI(),
        context.getBodyAsString(),
        Json.encode(error));

    originalRequest.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
    originalRequest.response().headers().setAll(originalRequest.headers());
    originalRequest.response().headers().remove("Content-Length"); // created during 'end'.
    originalRequest.response().setChunked(false);

    originalRequest.response().end(Json.encodeToBuffer(error));
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  private void logRequest(
      final RoutingContext context,
      final HttpClientRequest proxyRequest,
      final Buffer proxyRequestBody) {
    LOG.debug(
        "Original method: {}, uri: {}, body: {}, Proxy: method: {}, uri: {}, body: {}",
        context.request().method(),
        context.request().absoluteURI(),
        context.getBody(),
        proxyRequest.method(),
        proxyRequest.absoluteURI(),
        proxyRequestBody);
  }
}

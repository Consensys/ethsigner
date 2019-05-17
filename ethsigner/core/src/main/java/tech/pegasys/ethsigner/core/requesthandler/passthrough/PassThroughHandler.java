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
package tech.pegasys.ethsigner.core.requesthandler.passthrough;

import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;

import java.math.BigInteger;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcRequestHandler;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PassThroughHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpClient ethNodeClient;
  private final Duration httpRequestTimeout;

  public PassThroughHandler(final HttpClient ethNodeClient, final Duration httpRequestTimeout) {
    this.ethNodeClient = ethNodeClient;
    this.httpRequestTimeout = httpRequestTimeout;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    LOG.debug("Passing through request {}, {}", request.getId(), request.getMethod());
    final HttpServerRequest httpServerRequest = context.request();
    final HttpClientRequest proxyRequest =
        ethNodeClient.request(
            httpServerRequest.method(),
            httpServerRequest.uri(),
            response -> handleResponse(context, response));

    //proxyRequest.setTimeout(httpRequestTimeout.toMillis());
    //proxyRequest.exceptionHandler(thrown -> requestExceptionHandler(context, thrown));
    proxyRequest.headers().setAll(httpServerRequest.headers());
    proxyRequest.setChunked(false);

    proxyRequest.end(context.getBody());
    logRequest(request, httpServerRequest);
  }

  private void requestExceptionHandler(final RoutingContext context, final Throwable thrown) {
    if (thrown instanceof TimeoutException) {
      context.fail(GATEWAY_TIMEOUT.code());
    }
    // TODO: do we need to do something here, or will it fall through to the router's handler?
  }

  private void handleResponse(final RoutingContext context, final HttpClientResponse response) {
    context.vertx().executeBlocking(
        future -> {
          logResponse(response);
          response.bodyHandler(
              body -> {
                logResponseBody(body);
                handleResponseBody(context, response, body);
              });
        },
        false,
        (res) -> {
          if (res.failed()) {
            LOG.error(
                "An unhandled error occurred while processing {}",
                context.getBodyAsString(),
                res.cause());
          }
        });
  }

  private void handleResponseBody(
      final RoutingContext context, final HttpClientResponse response, final Buffer body) {
    context.request().response().setStatusCode(response.statusCode());
    context.request().response().headers().setAll(response.headers());
    context.request().response().setChunked(false);
    context.request().response().end(body);
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
        httpRequest::method,
        httpRequest::absoluteURI,
        () -> Json.encodePrettily(jsonRequest));
  }
}

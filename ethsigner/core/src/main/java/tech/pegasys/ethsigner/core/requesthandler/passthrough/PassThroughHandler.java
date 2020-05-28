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

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcRequestHandler;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.DownstreamPathCalculator;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PassThroughHandler implements JsonRpcRequestHandler, Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpClient ethNodeClient;
  private final VertxRequestTransmitter transmitter;
  final DownstreamPathCalculator downstreamPathCalculator;

  public PassThroughHandler(
      final HttpClient ethNodeClient,
      final VertxRequestTransmitterFactory vertxTransmitterFactory,
      final DownstreamPathCalculator downstreamPathCalculator) {
    transmitter = vertxTransmitterFactory.create(this::handleResponseBody);
    this.ethNodeClient = ethNodeClient;
    this.downstreamPathCalculator = downstreamPathCalculator;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    LOG.debug("Passing through request {}, {}", request.getId(), request.getMethod());
    handle(context);
  }

  @Override
  public void handle(final RoutingContext context) {
    final HttpServerRequest httpServerRequest = context.request();

    final HttpClientRequest proxyRequest =
        ethNodeClient.request(
            httpServerRequest.method(),
            downstreamPathCalculator.calculateDownstreamPath(httpServerRequest.uri()),
            response -> transmitter.handleResponse(context, response));

    final Buffer body = context.getBody();
    transmitter.sendRequest(proxyRequest, body, context);
    logRequest(httpServerRequest, body.toString());
  }

  private void handleResponseBody(
      final RoutingContext context, final HttpClientResponse response, final Buffer body) {
    context.request().response().setStatusCode(response.statusCode());
    context.request().response().headers().setAll(response.headers());
    context.request().response().setChunked(false);
    context.request().response().end(body);
  }

  private void logRequest(final HttpServerRequest httpRequest, final String body) {
    LOG.debug(
        "Proxying method: {}, uri: {}, body: {}",
        httpRequest::method,
        httpRequest::absoluteURI,
        () -> body);
  }
}

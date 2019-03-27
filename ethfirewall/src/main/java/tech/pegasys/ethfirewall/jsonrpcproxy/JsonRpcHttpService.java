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

import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;

import java.time.Duration;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcHttpService extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcHttpService.class);
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();

  private final RequestMapper requestHandlerMapper;
  private final HttpServerOptions serverOptions;
  private final Duration httpRequestTimeout;
  private HttpServer httpServer = null;

  public JsonRpcHttpService(
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final RequestMapper requestHandlerMapper) {
    this.serverOptions = serverOptions;
    this.httpRequestTimeout = httpRequestTimeout;
    this.requestHandlerMapper = requestHandlerMapper;
  }

  @Override
  public void start(final Future<Void> startFuture) {
    httpServer = vertx.createHttpServer(serverOptions);
    httpServer
        .requestHandler(router())
        .listen(
            result -> {
              if (result.succeeded()) {
                LOG.info("Json RPC server started on {}", httpServer.actualPort());
                startFuture.complete();
              } else {
                LOG.error("Json RPC server failed to listen", result.cause());
                startFuture.fail(result.cause());
              }
            });
  }

  @Override
  public void stop(final Future<Void> stopFuture) {
    httpServer.close(
        result -> {
          if (result.succeeded()) {
            stopFuture.complete();
          } else {
            stopFuture.fail(result.cause());
          }
        });
  }

  private Router router() {
    final Router router = Router.router(vertx);
    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .handler(TimeoutHandler.create(httpRequestTimeout.toMillis()))
        .failureHandler(new LogErrorHandler())
        .handler(this::handleJsonRpc);
    router.route().handler(context -> {});
    return router;
  }

  private void handleJsonRpc(final RoutingContext context) {
    try {
      final JsonObject json = context.getBodyAsJson();
      final Handler<RoutingContext> handler = requestHandlerMapper.getMatchingHandler(json);
      handler.handle(context);
    } catch (final DecodeException e) {
      sendParseErrorResponse(context, e);
    }
  }

  private void sendParseErrorResponse(final RoutingContext context, final Throwable error) {
    LOG.info("Dropping request from {}", context.request().remoteAddress());
    LOG.debug("Parsing body as JSON failed for: {}", context.getBodyAsString(), error);

    final HttpServerResponse response = context.response();
    response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code());

    response.end(Json.encodeToBuffer(new JsonRpcErrorResponse(JsonRpcError.PARSE_ERROR)));
  }
}

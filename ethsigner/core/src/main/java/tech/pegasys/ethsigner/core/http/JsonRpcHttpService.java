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
package tech.pegasys.ethsigner.core.http;

import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcRequestHandler;

import java.time.Duration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonRpcHttpService extends AbstractVerticle {

  private static final Logger LOG = LogManager.getLogger();
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private static final int UNASSIGNED_PORT = 0;

  static {
    // Force Jackson to fail when @JsonCreator values are missing
    Json.mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    Json.mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
  }

  private final RequestMapper requestHandlerMapper;
  private final HttpResponseFactory responseFactory;
  private final HttpServerOptions serverOptions;
  private final Duration httpRequestTimeout;
  private HttpServer httpServer;

  public JsonRpcHttpService(
      final HttpResponseFactory responseFactory,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final RequestMapper requestHandlerMapper) {
    this.responseFactory = responseFactory;
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

  public int actualPort() {
    if (httpServer == null) {
      return UNASSIGNED_PORT;
    }
    return httpServer.actualPort();
  }

  private Router router() {
    final Router router = Router.router(vertx);
    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .handler(TimeoutHandler.create(httpRequestTimeout.toMillis(), GATEWAY_TIMEOUT.code()))
        .failureHandler(new LogErrorHandler())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory()))
        .handler(this::handleJsonRpc);
    router.route().handler(context -> {});
    return router;
  }

  private void handleJsonRpc(final RoutingContext context) {
    vertx.executeBlocking(
        future -> {
          process(context);
          future.complete();
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

  private void process(final RoutingContext context) {
    try {
      LOG.trace("Request body = {}", context.getBodyAsString());

      final JsonObject requestJson = context.getBodyAsJson();
      final JsonRpcRequest request = requestJson.mapTo(JsonRpcRequest.class);
      final JsonRpcRequestHandler handler =
          requestHandlerMapper.getMatchingHandler(request.getMethod());
      handler.handle(context, request);
    } catch (final DecodeException | IllegalArgumentException e) {
      sendParseErrorResponse(context, e);
    }
  }

  private void sendParseErrorResponse(final RoutingContext context, final Throwable error) {
    LOG.info("Dropping request from {}", context.request().remoteAddress());
    LOG.debug("Parsing body as JSON failed for: {}", context.getBodyAsString(), error);
    responseFactory.create(
        context.request(),
        HttpResponseStatus.BAD_REQUEST.code(),
        new JsonRpcErrorResponse(JsonRpcError.PARSE_ERROR));
  }
}

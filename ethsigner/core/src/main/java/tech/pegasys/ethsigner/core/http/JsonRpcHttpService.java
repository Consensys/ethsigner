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

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonRpcHttpService extends AbstractVerticle {

  private static final Logger LOG = LogManager.getLogger();
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private static final String TEXT = HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8";
  private static final int UNASSIGNED_PORT = 0;

  private final HttpServerOptions serverOptions;
  private HttpServer httpServer;

  private final JsonRpcHandler jsonRpcHandler;

  public JsonRpcHttpService(
      final HttpResponseFactory responseFactory,
      final HttpServerOptions serverOptions,
      final RequestMapper requestHandlerMapper) {
    this.serverOptions = serverOptions;

    // TODO promote
    jsonRpcHandler = new JsonRpcHandler(responseFactory, requestHandlerMapper);
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

    // Handler for JSON-RPC requests
    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new LogErrorHandler())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory()))
        .handler(jsonRpcHandler);

    // Handler for UpCheck endpoint
    router
        .route(HttpMethod.GET, "/upcheck")
        .produces(TEXT)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new LogErrorHandler())
        .handler(new UpcheckHandler());

    // Default route handler does nothing: no response
    router.route().handler(context -> {});
    return router;
  }
}

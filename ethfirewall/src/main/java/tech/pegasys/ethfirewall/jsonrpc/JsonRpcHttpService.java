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
package tech.pegasys.ethfirewall.jsonrpc;

import tech.pegasys.ethfirewall.config.EthFirewallConfig;

import java.util.function.Function;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcHttpService extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcHttpService.class);
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();

  private EthFirewallConfig config;
  private Function<String, Handler<RoutingContext>> handler;
  private HttpServer httpServer;

  public JsonRpcHttpService(
      final EthFirewallConfig config, final Function<String, Handler<RoutingContext>> handler) {
    this.config = config;
    this.handler = handler;
  }

  @Override
  public void start(final Future<Void> startFuture) {
    final HttpServerOptions serverOptions =
        new HttpServerOptions().setPort(config.getPort()).setHost(config.getHost());
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
        .failureHandler(new LogErrorHandler())
        .handler(this::handleJsonRpc);
    router.route().handler(context -> {});
    return router;
  }

  private void handleJsonRpc(final RoutingContext routingContext) {
    final String method = routingContext.getBodyAsJson().getString("method");
    handler.apply(method).handle(routingContext);
  }
}

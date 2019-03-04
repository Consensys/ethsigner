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
package tech.pegasys.ethfirewall.reverseproxy;

import tech.pegasys.ethfirewall.config.EthFirewallConfig;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides reverse proxy functionality for JSON-RPC. */
public class JsonRpcReverseProxy extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcReverseProxy.class);
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private HttpServer proxy;
  private EthFirewallConfig config;

  public JsonRpcReverseProxy(EthFirewallConfig config) {
    this.config = config;
  }

  @Override
  public void start(final Future<Void> startFuture) {
    startServer(router(), startFuture);
  }

  @Override
  public void stop(final Future<Void> stopFuture) {
    proxy.close(
        result -> {
          if (result.succeeded()) {
            stopFuture.complete();
          } else {
            stopFuture.fail(result.cause());
          }
        });
  }

  private Router router() {
    final WebClientOptions clientOptions =
        new WebClientOptions()
            .setDefaultPort(config.getEthPort())
            .setDefaultHost(config.getEthHost());
    final ReverseProxy reverseProxy = new ReverseProxy(vertx.createHttpClient(clientOptions));
    final Router router = Router.router(vertx);
    addJsonRpcRoute(router, reverseProxy);
    addDefaultRoute(router);
    return router;
  }

  /**
   * As JSON-RPC requests are currently only POST, reject other methods
   *
   * <p>https://github.com/ethereum/wiki/wiki/JSON-RPC
   */
  private void addJsonRpcRoute(final Router router, final ReverseProxy ethereumNode) {
    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new LogErrorHandler())
        .handler(new ProxyHandler(ethereumNode));
  }

  /** Default security response; send back no response */
  private void addDefaultRoute(final Router router) {
    router.route().handler(context -> {});
  }

  private void startServer(final Router router, final Future<Void> startFuture) {
    final HttpServerOptions serverOptions =
        new HttpServerOptions().setPort(config.getPort()).setHost(config.getHost());
    this.proxy = vertx.createHttpServer(serverOptions);

    proxy
        .requestHandler(router)
        .listen(
            result -> {
              if (result.succeeded()) {
                LOG.info("Reverse proxy server started on {}", proxy.actualPort());
                startFuture.complete();
              } else {
                LOG.error("Reverse proxy server failed to listen", result.cause());
                startFuture.fail(result.cause());
              }
            });
  }
}

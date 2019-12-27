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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpServerService extends AbstractVerticle {

  private static final Logger LOG = LogManager.getLogger();
  private static final int UNASSIGNED_PORT = 0;

  private final HttpServerOptions serverOptions;
  private final Router routes;
  private HttpServer httpServer;

  public HttpServerService(final Router routes, final HttpServerOptions serverOptions) {
    this.serverOptions = serverOptions;
    this.routes = routes;
  }

  @Override
  public void start(final Future<Void> startFuture) {
    httpServer = vertx.createHttpServer(serverOptions);
    try {
      httpServer
          .requestHandler(routes)
          .listen(
              result -> {
                if (result.succeeded()) {
                  LOG.info("HTTP server service started on {}", httpServer.actualPort());
                  startFuture.complete();
                } else {
                  LOG.error("HTTP server service failed to listen", result.cause());
                  startFuture.fail(result.cause());
                }
              });
    } catch (final Exception e) {
      startFuture.fail(e);
      throw e;
    }
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
}

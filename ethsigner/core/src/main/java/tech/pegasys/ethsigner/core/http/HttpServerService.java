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

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpServerService {

  private static final Logger LOG = LogManager.getLogger();
  private final Handler<HttpServerRequest> requestHandler;

  private HttpServer httpServer;

  public HttpServerService(final Handler<HttpServerRequest> requestHandler,
      final HttpServer httpServer) {
    this.httpServer = httpServer;
    this.requestHandler = requestHandler;
  }

  public void waitUntilStarted() throws ExecutionException, InterruptedException {
    final CompletableFuture<Void> serverStartupComplete = new CompletableFuture<>();
    httpServer
        .requestHandler(requestHandler)
        .listen(
            result -> {
              if (result.succeeded()) {
                LOG.info("HTTP server service started on {}", httpServer.actualPort());
                serverStartupComplete.complete(null);
              } else {
                LOG.error("HTTP server service failed to listen", result.cause());
                serverStartupComplete.completeExceptionally(result.cause());
              }
            });
    serverStartupComplete.get();
  }

  public int actualPort() {
    return httpServer.actualPort();
  }
}

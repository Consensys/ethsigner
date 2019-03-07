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
package tech.pegasys.ethfirewall;

import tech.pegasys.ethfirewall.jsonrpcproxy.JsonRpcHttpService;
import tech.pegasys.ethfirewall.jsonrpcproxy.PassThroughHandler;
import tech.pegasys.ethfirewall.jsonrpcproxy.RequestMapper;
import tech.pegasys.ethfirewall.jsonrpcproxy.TransactionBodyProvider;
import tech.pegasys.ethfirewall.jsonrpcproxy.TransactionSigner;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {

  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);
  private TransactionSigner transactionSigner;
  private HttpClientOptions clientOptions;
  private HttpServerOptions serverOptions;

  public Runner(
      final TransactionSigner transactionSigner,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions) {
    this.transactionSigner = transactionSigner;
    this.clientOptions = clientOptions;
    this.serverOptions = serverOptions;
  }

  public void start() {
    // NOTE: Starting vertx spawns daemon threads, meaning the app may complete, but not terminate.
    final Vertx vertx = Vertx.vertx();
    final RequestMapper requestMapper = createRequestMapper(vertx, transactionSigner);
    final JsonRpcHttpService httpService = new JsonRpcHttpService(serverOptions, requestMapper);

    vertx.deployVerticle(httpService, this::handleDeployResult);
  }

  private RequestMapper createRequestMapper(
      final Vertx vertx, final TransactionSigner transactionSigner) {

    final HttpClient downStreamConnection = vertx.createHttpClient(clientOptions);
    final PassThroughHandler passThroughHandler =
        new PassThroughHandler(downStreamConnection, RoutingContext::getBody);

    final RequestMapper requestMapper = new RequestMapper(passThroughHandler);

    final TransactionBodyProvider sendTransactionHandler =
        new TransactionBodyProvider(transactionSigner);

    requestMapper.addHandler(
        "eth_sendTransaction",
        new PassThroughHandler(downStreamConnection, sendTransactionHandler));

    return requestMapper;
  }

  private void handleDeployResult(final AsyncResult<?> result) {
    if (result.succeeded()) {
      LOG.info("Deployment id is: {}", result.result());
    } else {
      LOG.warn("Deployment failed! ", result.cause());
    }
  }
}

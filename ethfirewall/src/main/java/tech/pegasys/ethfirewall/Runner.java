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

import tech.pegasys.ethfirewall.jsonrpcproxy.EthAccountsBodyProvider;
import tech.pegasys.ethfirewall.jsonrpcproxy.HttpResponseFactory;
import tech.pegasys.ethfirewall.jsonrpcproxy.InternalResponseHandler;
import tech.pegasys.ethfirewall.jsonrpcproxy.JsonRpcErrorReporter;
import tech.pegasys.ethfirewall.jsonrpcproxy.JsonRpcHttpService;
import tech.pegasys.ethfirewall.jsonrpcproxy.PassThroughHandler;
import tech.pegasys.ethfirewall.jsonrpcproxy.RequestMapper;
import tech.pegasys.ethfirewall.jsonrpcproxy.SendTransactionBodyProvider;
import tech.pegasys.ethfirewall.jsonrpcproxy.SendTransactionHandler;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import java.time.Duration;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {

  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);
  private final TransactionSigner transactionSigner;
  private final HttpClientOptions clientOptions;
  private final HttpServerOptions serverOptions;
  private final Duration httpRequestTimeout;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final JsonRpcErrorReporter errorReporter = new JsonRpcErrorReporter(responseFactory);

  private Vertx vertx;
  private String deploymentId;

  public Runner(
      final TransactionSigner transactionSigner,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout) {
    this.transactionSigner = transactionSigner;
    this.clientOptions = clientOptions;
    this.serverOptions = serverOptions;
    this.httpRequestTimeout = httpRequestTimeout;
  }

  public void start() {
    // NOTE: Starting vertx spawns daemon threads, meaning the app may complete, but not terminate.
    vertx = Vertx.vertx();
    final RequestMapper requestMapper = createRequestMapper(vertx, transactionSigner);
    final JsonRpcHttpService httpService =
        new JsonRpcHttpService(responseFactory, serverOptions, httpRequestTimeout, requestMapper);
    vertx.deployVerticle(httpService, this::handleDeployResult);
  }

  public void stop() {
    vertx.undeploy(deploymentId);
  }

  private RequestMapper createRequestMapper(
      final Vertx vertx, final TransactionSigner transactionSigner) {

    final HttpClient downStreamConnection = vertx.createHttpClient(clientOptions);

    final RequestMapper requestMapper =
        new RequestMapper(new PassThroughHandler(downStreamConnection));

    requestMapper.addHandler(
        "eth_sendTransaction",
        new SendTransactionHandler(
            errorReporter,
            downStreamConnection,
            new SendTransactionBodyProvider(transactionSigner)));

    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler(
            responseFactory,
            new EthAccountsBodyProvider(transactionSigner.getAddress()),
            errorReporter));

    return requestMapper;
  }

  private void handleDeployResult(final AsyncResult<String> result) {
    if (result.succeeded()) {
      deploymentId = result.result();
      LOG.info("Vertx deployment id is: {}", deploymentId);
    } else {
      LOG.error("Vertx deployment failed", result.cause());
      System.exit(1);
    }
  }
}

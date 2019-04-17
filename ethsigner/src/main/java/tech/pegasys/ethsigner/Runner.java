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
package tech.pegasys.ethsigner;

import tech.pegasys.ethsigner.http.HttpResponseFactory;
import tech.pegasys.ethsigner.http.JsonRpcHttpService;
import tech.pegasys.ethsigner.http.RequestMapper;
import tech.pegasys.ethsigner.requesthandler.JsonRpcErrorReporter;
import tech.pegasys.ethsigner.requesthandler.internalresponse.EthAccountsBodyProvider;
import tech.pegasys.ethsigner.requesthandler.internalresponse.InternalResponseHandler;
import tech.pegasys.ethsigner.requesthandler.passthrough.PassThroughHandler;
import tech.pegasys.ethsigner.requesthandler.sendtransaction.RawTransactionConverter;
import tech.pegasys.ethsigner.requesthandler.sendtransaction.SendTransactionHandler;
import tech.pegasys.ethsigner.signing.TransactionSerialiser;

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
  private final TransactionSerialiser serialiser;
  private final HttpClientOptions clientOptions;
  private final HttpServerOptions serverOptions;
  private final Duration httpRequestTimeout;
  private final RawTransactionConverter transactionConverter;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final JsonRpcErrorReporter errorReporter = new JsonRpcErrorReporter(responseFactory);

  private Vertx vertx;
  private String deploymentId;

  public Runner(
      final TransactionSerialiser serialiser,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final RawTransactionConverter transactionConverter) {
    this.serialiser = serialiser;
    this.clientOptions = clientOptions;
    this.serverOptions = serverOptions;
    this.httpRequestTimeout = httpRequestTimeout;
    this.transactionConverter = transactionConverter;
  }

  public void start() {
    // NOTE: Starting vertx spawns daemon threads, meaning the app may complete, but not terminate.
    vertx = Vertx.vertx();
    final RequestMapper requestMapper = createRequestMapper(vertx);
    final JsonRpcHttpService httpService =
        new JsonRpcHttpService(responseFactory, serverOptions, httpRequestTimeout, requestMapper);
    vertx.deployVerticle(httpService, this::handleDeployResult);
  }

  public void stop() {
    vertx.undeploy(deploymentId);
  }

  private RequestMapper createRequestMapper(final Vertx vertx) {

    final HttpClient downStreamConnection = vertx.createHttpClient(clientOptions);

    final RequestMapper requestMapper =
        new RequestMapper(new PassThroughHandler(downStreamConnection));

    requestMapper.addHandler(
        "eth_sendTransaction",
        new SendTransactionHandler(
            errorReporter, downStreamConnection, serialiser, transactionConverter));

    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler(
            responseFactory, new EthAccountsBodyProvider(serialiser.getAddress()), errorReporter));

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

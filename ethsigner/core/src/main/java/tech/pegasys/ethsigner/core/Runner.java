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
package tech.pegasys.ethsigner.core;

import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.http.JsonRpcHttpService;
import tech.pegasys.ethsigner.core.http.RequestMapper;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthAccountsBodyProvider;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.InternalResponseHandler;
import tech.pegasys.ethsigner.core.requesthandler.passthrough.PassThroughHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.SendTransactionHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Runner {

  private static final Logger LOG = LogManager.getLogger();

  private final TransactionSerialiser serialiser;
  private final HttpClientOptions clientOptions;
  private final HttpServerOptions serverOptions;
  private final Duration httpRequestTimeout;
  private final TransactionFactory transactionFactory;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final Path dataDirectory;

  private Vertx vertx;
  private String deploymentId;
  private JsonRpcHttpService httpService;

  public Runner(
      final TransactionSerialiser serialiser,
      final Vertx vertx,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final TransactionFactory transactionFactory,
      final Path dataDirectory) {
    this.serialiser = serialiser;
    this.clientOptions = clientOptions;
    this.serverOptions = serverOptions;
    this.httpRequestTimeout = httpRequestTimeout;
    this.transactionFactory = transactionFactory;
    this.dataDirectory = dataDirectory;
    this.vertx = vertx;
  }

  public void start() {
    final RequestMapper requestMapper = createRequestMapper(vertx);
    httpService =
        new JsonRpcHttpService(responseFactory, serverOptions, httpRequestTimeout, requestMapper);
    vertx.deployVerticle(httpService, this::handleDeployResult);
  }

  public void stop() {
    vertx.undeploy(deploymentId);
  }

  private RequestMapper createRequestMapper(final Vertx vertx) {

    final HttpClient downStreamConnection = vertx.createHttpClient(clientOptions);

    final RequestMapper requestMapper =
        new RequestMapper(
            new PassThroughHandler(
                downStreamConnection,
                responseBodyHandler ->
                    new VertxRequestTransmitter(httpRequestTimeout, responseBodyHandler)));

    final SendTransactionHandler sendTransactionHandler =
        new SendTransactionHandler(
            downStreamConnection,
            serialiser,
            transactionFactory,
            responseBodyHandler ->
                new VertxRequestTransmitter(httpRequestTimeout, responseBodyHandler));
    requestMapper.addHandler("eth_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler("eea_sendTransaction", sendTransactionHandler);

    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler(
            responseFactory, new EthAccountsBodyProvider(serialiser.getAddress())));

    return requestMapper;
  }

  private void handleDeployResult(final AsyncResult<String> result) {
    if (result.succeeded()) {
      deploymentId = result.result();
      LOG.info("Vertx deployment id is: {}", deploymentId);

      if (dataDirectory != null) {
        writePortsToFile(httpService);
      }
    } else {
      LOG.error("Vertx deployment failed", result.cause());
      System.exit(1);
    }
  }

  private void writePortsToFile(final JsonRpcHttpService httpService) {
    final File portsFile = new File(dataDirectory.toFile(), "ethsigner.ports");
    portsFile.deleteOnExit();

    final Properties properties = new Properties();
    properties.setProperty("http-jsonrpc", String.valueOf(httpService.actualPort()));

    LOG.info(
        "Writing ethsigner.ports file: {}, with contents: {}",
        portsFile.getAbsolutePath(),
        properties);
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Pantheon. This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      LOG.warn("Error writing ports file", e);
    }
  }
}

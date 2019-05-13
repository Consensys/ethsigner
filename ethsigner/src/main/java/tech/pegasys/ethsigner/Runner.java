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
import tech.pegasys.ethsigner.requesthandler.internalresponse.EthAccountsBodyProvider;
import tech.pegasys.ethsigner.requesthandler.internalresponse.InternalResponseHandler;
import tech.pegasys.ethsigner.requesthandler.passthrough.PassThroughHandler;
import tech.pegasys.ethsigner.requesthandler.sendtransaction.NonceProvider;
import tech.pegasys.ethsigner.requesthandler.sendtransaction.SendTransactionHandler;
import tech.pegasys.ethsigner.signing.TransactionSerialiser;

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
  private final NonceProvider nonceProvider;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final Path dataDirectory;

  private Vertx vertx;
  private String deploymentId;
  private JsonRpcHttpService httpService;

  public Runner(
      final TransactionSerialiser serialiser,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final NonceProvider nonceProvider,
      final Path dataDirectory) {
    this.serialiser = serialiser;
    this.clientOptions = clientOptions;
    this.serverOptions = serverOptions;
    this.httpRequestTimeout = httpRequestTimeout;
    this.nonceProvider = nonceProvider;
    this.dataDirectory = dataDirectory;
  }

  public void start() {
    // NOTE: Starting vertx spawns daemon threads, meaning the app may complete, but not terminate.
    vertx = Vertx.vertx();
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
        new RequestMapper(new PassThroughHandler(downStreamConnection));

    requestMapper.addHandler(
        "eth_sendTransaction",
        new SendTransactionHandler(
            responseFactory, downStreamConnection, serialiser, nonceProvider));

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
    final Properties properties = new Properties();

    properties.setProperty("http-jsonrpc", String.valueOf(httpService.actualPort()));

    final File portsFile = new File(dataDirectory.toFile(), "ethsigner.ports");
    portsFile.deleteOnExit();

    LOG.info("Writing ethsigner.ports file: {}", portsFile.getAbsolutePath());
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Pantheon. This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      LOG.warn("Error writing ports file", e);
    }
  }
}

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
import tech.pegasys.ethsigner.core.http.HttpServerService;
import tech.pegasys.ethsigner.core.http.JsonRpcErrorHandler;
import tech.pegasys.ethsigner.core.http.JsonRpcHandler;
import tech.pegasys.ethsigner.core.http.LogErrorHandler;
import tech.pegasys.ethsigner.core.http.RequestMapper;
import tech.pegasys.ethsigner.core.http.UpcheckHandler;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthAccountsBodyProvider;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.InternalResponseHandler;
import tech.pegasys.ethsigner.core.requesthandler.passthrough.PassThroughHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.SendTransactionHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Runner {

  private static final Logger LOG = LogManager.getLogger();
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private static final String TEXT = HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8";

  private final long chainId;
  private final TransactionSignerProvider transactionSignerProvider;
  private final HttpClientOptions clientOptions;
  private final Duration httpRequestTimeout;
  private final TransactionFactory transactionFactory;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final Path dataPath;
  private final Vertx vertx;
  private final HttpServerService httpServerService;

  public Runner(
      final long chainId,
      final TransactionSignerProvider transactionSignerProvider,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final TransactionFactory transactionFactory,
      final Path dataPath) {
    this.chainId = chainId;
    this.transactionSignerProvider = transactionSignerProvider;
    this.clientOptions = clientOptions;
    this.httpRequestTimeout = httpRequestTimeout;
    this.transactionFactory = transactionFactory;
    this.dataPath = dataPath;
    this.vertx = Vertx.vertx();
    this.httpServerService = new HttpServerService(router(), serverOptions);
  }

  public void start() {
    vertx.deployVerticle(httpServerService, this::httpServerServiceDeployment);
  }

  public void stop() {
    vertx.close();
  }

  private RequestMapper createRequestMapper() {

    final HttpClient downStreamConnection = vertx.createHttpClient(clientOptions);

    final RequestMapper requestMapper =
        new RequestMapper(
            new PassThroughHandler(
                downStreamConnection,
                responseBodyHandler ->
                    new VertxRequestTransmitter(httpRequestTimeout, responseBodyHandler)));

    final SendTransactionHandler sendTransactionHandler =
        new SendTransactionHandler(
            chainId,
            downStreamConnection,
            transactionSignerProvider,
            transactionFactory,
            responseBodyHandler ->
                new VertxRequestTransmitter(httpRequestTimeout, responseBodyHandler));
    requestMapper.addHandler("eth_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler("eea_sendTransaction", sendTransactionHandler);

    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler(
            responseFactory,
            new EthAccountsBodyProvider(transactionSignerProvider::availableAddresses)));

    return requestMapper;
  }

  private Router router() {
    final Router router = Router.router(vertx);
    final RequestMapper requestMapper = createRequestMapper();

    // Handler for JSON-RPC requests
    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new LogErrorHandler())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory()))
        .handler(new JsonRpcHandler(responseFactory, requestMapper));

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

  private void httpServerServiceDeployment(final AsyncResult<String> result) {
    if (result.succeeded()) {
      LOG.info("JsonRpcHttpService Vertx deployment id is: {}", result.result());

      if (dataPath != null) {
        writePortsToFile(httpServerService);
      }
    } else {
      deploymentFailed(result.cause());
    }
  }

  private void deploymentFailed(final Throwable cause) {
    LOG.error("Vertx deployment failed", cause);
    System.exit(1);
  }

  private void writePortsToFile(final HttpServerService httpService) {
    final File portsFile = new File(dataPath.toFile(), "ethsigner.ports");
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

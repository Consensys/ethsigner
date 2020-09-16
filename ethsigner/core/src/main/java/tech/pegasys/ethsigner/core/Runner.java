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
import tech.pegasys.ethsigner.core.http.JsonRpcErrorHandler;
import tech.pegasys.ethsigner.core.http.JsonRpcHandler;
import tech.pegasys.ethsigner.core.http.LogErrorHandler;
import tech.pegasys.ethsigner.core.http.RequestMapper;
import tech.pegasys.ethsigner.core.http.UpcheckHandler;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.metrics.MetricsEndpoint;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthAccountsResultProvider;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthSignResultProvider;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthSignTransactionResultProvider;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.InternalResponseHandler;
import tech.pegasys.ethsigner.core.requesthandler.passthrough.PassThroughHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.DownstreamPathCalculator;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.SendTransactionHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.TransactionFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.Sets;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Runner {

  private static final Logger LOG = LogManager.getLogger();
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private static final String TEXT = HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8";

  private final long chainId;
  private final AddressIndexedSignerProvider signerProvider;
  private final HttpClientOptions clientOptions;
  private final Duration httpRequestTimeout;
  private final DownstreamPathCalculator downstreamPathCalculator;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final JsonDecoder jsonDecoder;
  private final Path dataPath;
  private final Vertx vertx;
  private final Collection<String> allowedCorsOrigins;
  private final HttpServerOptions serverOptions;
  private final MetricsEndpoint metricsEndpoint;

  public Runner(
      final long chainId,
      final AddressIndexedSignerProvider signerProvider,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final DownstreamPathCalculator downstreamPathCalculator,
      final JsonDecoder jsonDecoder,
      final Path dataPath,
      final Vertx vertx,
      final Collection<String> allowedCorsOrigins,
      final MetricsEndpoint metricsEndpoint) {
    this.chainId = chainId;
    this.signerProvider = signerProvider;
    this.clientOptions = clientOptions;
    this.httpRequestTimeout = httpRequestTimeout;
    this.downstreamPathCalculator = downstreamPathCalculator;
    this.jsonDecoder = jsonDecoder;
    this.dataPath = dataPath;
    this.vertx = vertx;
    this.allowedCorsOrigins = allowedCorsOrigins;
    this.serverOptions = serverOptions;
    this.metricsEndpoint = metricsEndpoint;
  }

  public void start() throws ExecutionException, InterruptedException {
    metricsEndpoint.start(vertx);
    final HttpServer httpServer = createServerAndWait(vertx, router());
    LOG.info("Server is up, and listening on {}", httpServer.actualPort());
    if (dataPath != null) {
      writePortsToFile(httpServer, metricsEndpoint.getPort());
    }
  }

  private Router router() {
    final HttpClient downStreamConnection = vertx.createHttpClient(clientOptions);
    final VertxRequestTransmitterFactory transmitterFactory =
        responseBodyHandler ->
            new VertxRequestTransmitter(
                vertx,
                downStreamConnection,
                httpRequestTimeout,
                downstreamPathCalculator,
                responseBodyHandler);
    final RequestMapper requestMapper = createRequestMapper(transmitterFactory);

    final Router router = Router.router(vertx);

    // Handler for JSON-RPC requests
    router
        .route()
        .handler(
            CorsHandler.create(buildCorsRegexFromConfig())
                .allowedHeaders(Sets.newHashSet("*", "content-type")));

    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory()))
        .blockingHandler(new JsonRpcHandler(responseFactory, requestMapper, jsonDecoder), false);

    // Handler for UpCheck endpoint
    router
        .route(HttpMethod.GET, "/upcheck")
        .produces(TEXT)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new LogErrorHandler())
        .handler(new UpcheckHandler());

    final PassThroughHandler passThroughHandler = new PassThroughHandler(transmitterFactory);
    router.route().handler(BodyHandler.create()).handler(passThroughHandler);
    return router;
  }

  private RequestMapper createRequestMapper(
      final VertxRequestTransmitterFactory transmitterFactory) {
    final PassThroughHandler defaultHandler = new PassThroughHandler(transmitterFactory);
    final TransactionFactory transactionFactory =
        new TransactionFactory(jsonDecoder, transmitterFactory);

    final SendTransactionHandler sendTransactionHandler =
        new SendTransactionHandler(chainId, signerProvider, transactionFactory, transmitterFactory);

    final RequestMapper requestMapper = new RequestMapper(defaultHandler);
    requestMapper.addHandler("eth_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler("eea_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler<>(
            responseFactory, new EthAccountsResultProvider(signerProvider::availablePublicKeys)));
    requestMapper.addHandler(
        "eth_sign",
        new InternalResponseHandler<>(responseFactory, new EthSignResultProvider(signerProvider)));
    requestMapper.addHandler(
        "eth_signTransaction",
        new InternalResponseHandler<>(
            responseFactory,
            new EthSignTransactionResultProvider(chainId, signerProvider, jsonDecoder)));
    return requestMapper;
  }

  private void writePortsToFile(final HttpServer server, final Optional<Integer> metricsPort) {
    final File portsFile = new File(dataPath.toFile(), "ethsigner.ports");
    portsFile.deleteOnExit();

    final Properties properties = new Properties();
    properties.setProperty("http-jsonrpc", String.valueOf(server.actualPort()));
    metricsPort.ifPresent(port -> properties.setProperty("metrics-port", String.valueOf(port)));

    LOG.info(
        "Writing ethsigner.ports file: {}, with contents: {}",
        portsFile.getAbsolutePath(),
        properties);
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Web3Provider. This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      LOG.warn("Error writing ports file", e);
    }
  }

  private String buildCorsRegexFromConfig() {
    if (allowedCorsOrigins.isEmpty()) {
      return "";
    }
    if (allowedCorsOrigins.contains("*")) {
      return "*";
    } else {
      final StringJoiner stringJoiner = new StringJoiner("|");
      allowedCorsOrigins.stream().filter(s -> !s.isEmpty()).forEach(stringJoiner::add);
      return stringJoiner.toString();
    }
  }

  private HttpServer createServerAndWait(
      final Vertx vertx, final Handler<HttpServerRequest> requestHandler)
      throws ExecutionException, InterruptedException {

    final HttpServer httpServer = vertx.createHttpServer(serverOptions);
    final CompletableFuture<Void> serverRunningFuture = new CompletableFuture<>();
    httpServer
        .requestHandler(requestHandler)
        .listen(
            result -> {
              if (result.succeeded()) {
                serverRunningFuture.complete(null);
              } else {
                LOG.error(
                    "Failed to create HTTP Server on {}:{}",
                    serverOptions.getHost(),
                    serverOptions.getPort());
                serverRunningFuture.completeExceptionally(result.cause());
              }
            });
    serverRunningFuture.get();

    return httpServer;
  }
}

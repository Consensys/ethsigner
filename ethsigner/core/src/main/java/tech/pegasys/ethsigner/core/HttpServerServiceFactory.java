/*
 * Copyright 2020 ConsenSys AG.
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

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.http.HttpServerService;
import tech.pegasys.ethsigner.core.http.JsonRpcErrorHandler;
import tech.pegasys.ethsigner.core.http.JsonRpcHandler;
import tech.pegasys.ethsigner.core.http.LogErrorHandler;
import tech.pegasys.ethsigner.core.http.RequestMapper;
import tech.pegasys.ethsigner.core.http.UpcheckHandler;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthAccountsBodyProvider;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.InternalResponseHandler;
import tech.pegasys.ethsigner.core.requesthandler.passthrough.PassThroughHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.SendTransactionHandler;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.VertxNonceRequestTransmitterFactory;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class HttpServerServiceFactory {

  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private static final String TEXT = HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8";

  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final Vertx vertx;
  private final JsonDecoder jsonDecoder;

  public HttpServerServiceFactory(final Vertx vertx, final JsonDecoder jsonDecoder) {
    this.vertx = vertx;
    this.jsonDecoder = jsonDecoder;
  }

  public HttpServerService create(final Context context) {
    final HttpServer httpServer = vertx.createHttpServer(context.getServerOptions());
    return new HttpServerService(requestHandler(context), httpServer);
  }

  private Handler<HttpServerRequest> requestHandler(final Context context) {
    final HttpClient downStreamConnection = vertx.createHttpClient(context.getClientOptions());
    final VertxRequestTransmitterFactory transmitterFactory =
        responseBodyHandler ->
            new VertxRequestTransmitter(context.getHttpRequestTimeout(), responseBodyHandler);
    final RequestMapper requestMapper =
        createRequestMapper(context, downStreamConnection, transmitterFactory);

    final Router router = Router.router(vertx);

    // Handler for JSON-RPC requests
    router
        .route(HttpMethod.POST, "/")
        .produces(JSON)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory(), jsonDecoder))
        .handler(new JsonRpcHandler(responseFactory, requestMapper, jsonDecoder));

    // Handler for UpCheck endpoint
    router
        .route(HttpMethod.GET, "/upcheck")
        .produces(TEXT)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new LogErrorHandler())
        .handler(new UpcheckHandler());

    final PassThroughHandler passThroughHandler =
        new PassThroughHandler(downStreamConnection, transmitterFactory);
    router.route().handler(BodyHandler.create()).handler(passThroughHandler);
    return router;
  }

  private RequestMapper createRequestMapper(
      final Context context,
      final HttpClient downStreamConnection,
      final VertxRequestTransmitterFactory transmitterFactory) {

    final PassThroughHandler defaultHandler =
        new PassThroughHandler(downStreamConnection, transmitterFactory);

    final VertxNonceRequestTransmitterFactory nonceRequestTransmitterFactory =
        new VertxNonceRequestTransmitterFactory(
            downStreamConnection, jsonDecoder, context.getHttpRequestTimeout());

    final TransactionFactory transactionFactory =
        new TransactionFactory(jsonDecoder, nonceRequestTransmitterFactory);

    final SendTransactionHandler sendTransactionHandler =
        new SendTransactionHandler(
            context.getChainId(),
            downStreamConnection,
            context.getTransactionSignerProvider(),
            transactionFactory,
            transmitterFactory);

    final RequestMapper requestMapper = new RequestMapper(defaultHandler);
    requestMapper.addHandler("eth_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler("eea_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler(
            responseFactory,
            new EthAccountsBodyProvider(
                () -> context.getTransactionSignerProvider().availableAddresses()),
            jsonDecoder));

    return requestMapper;
  }
}

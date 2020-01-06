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
package tech.pegasys.ethsigner.tests.tls.support;

import static tech.pegasys.ethsigner.core.EthSigner.createJsonDecoder;
import static tech.pegasys.ethsigner.tests.dsl.tls.OkHttpClientHelpers.populateFingerprintFile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.http.JsonRpcErrorHandler;
import tech.pegasys.ethsigner.core.http.JsonRpcHandler;
import tech.pegasys.ethsigner.core.http.RequestMapper;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.tuweni.net.tls.VertxTrustOptions;

public class TlsEnabledHttpServer {

  // TODO: Need to kill the Vertx created in here :(

  public static HttpServer createServer(
      final TlsCertificateDefinition serverCert,
      final TlsCertificateDefinition acceptedClientCerts,
      final Path workDir)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, ExecutionException, InterruptedException {

    final Path serverFingerprintFile = workDir.resolve("server_known_clients");
    populateFingerprintFile(serverFingerprintFile, acceptedClientCerts);

    final HttpServerOptions web3HttpServerOptions = new HttpServerOptions();
    web3HttpServerOptions.setSsl(true);
    web3HttpServerOptions.setClientAuth(ClientAuth.REQUIRED);
    web3HttpServerOptions.setTrustOptions(
        VertxTrustOptions.whitelistClients(serverFingerprintFile));
    web3HttpServerOptions.setPort(0);
    web3HttpServerOptions.setPfxKeyCertOptions(
        new PfxOptions()
            .setPath(serverCert.getPkcs12File().toString())
            .setPassword(serverCert.getPassword()));

    final Vertx vertx = Vertx.vertx();

    final Router router = Router.router(vertx);
    final JsonDecoder jsonDecoder = createJsonDecoder();
    final RequestMapper requestMapper = new RequestMapper(new MockBalanceReporter());
    router
        .route(HttpMethod.POST, "/")
        .produces(HttpHeaderValues.APPLICATION_JSON.toString())
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory(), jsonDecoder))
        .handler(new JsonRpcHandler(null, requestMapper, jsonDecoder));

    final HttpServer web3ProviderhttpServer = vertx.createHttpServer(web3HttpServerOptions);

    final CompletableFuture<Boolean> serverConfigured = new CompletableFuture<>();
    web3ProviderhttpServer.requestHandler(router).listen(result -> serverConfigured.complete(true));

    serverConfigured.get();

    return web3ProviderhttpServer;
  }
}

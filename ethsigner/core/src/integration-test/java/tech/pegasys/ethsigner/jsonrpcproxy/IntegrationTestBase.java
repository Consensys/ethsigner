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
package tech.pegasys.ethsigner.jsonrpcproxy;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
import static org.web3j.utils.Async.defaultExecutorService;

import tech.pegasys.ethsigner.Runner;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.Web3jNonceProvider;
import tech.pegasys.ethsigner.core.signing.FileBasedTransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.jsonrpcproxy.model.request.EthNodeRequest;
import tech.pegasys.ethsigner.jsonrpcproxy.model.request.EthRequestFactory;
import tech.pegasys.ethsigner.jsonrpcproxy.model.request.EthSignerRequest;
import tech.pegasys.ethsigner.jsonrpcproxy.model.response.EthNodeResponse;
import tech.pegasys.ethsigner.jsonrpcproxy.model.response.EthResponseFactory;
import tech.pegasys.ethsigner.jsonrpcproxy.model.response.EthSignerResponse;
import tech.pegasys.ethsigner.jsonrpcproxy.support.EthTransactionCountResponder;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Delay;
import org.mockserver.model.Header;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;

public class IntegrationTestBase {

  private static final Logger LOG = LogManager.getLogger();
  private static final String LOCALHOST = "127.0.0.1";
  private static final long DEFAULT_CHAIN_ID = 9;

  protected static final String MALFORMED_JSON = "{Bad Json: {{{}";

  private static Runner runner;
  protected static ClientAndServer clientAndServer;

  private JsonRpc2_0Web3j jsonRpc;

  protected final EthRequestFactory request = new EthRequestFactory();
  protected final EthResponseFactory response = new EthResponseFactory();

  protected static String unlockedAccount;

  protected static Duration downstreamTimeout = Duration.ofSeconds(1);

  @BeforeClass
  public static void setupEthSigner() throws IOException, CipherException {
    setupEthSigner(DEFAULT_CHAIN_ID);
  }

  protected static void setupEthSigner(final long chainId) throws IOException, CipherException {
    clientAndServer = startClientAndServer();

    final TransactionSerialiser serialiser =
        new TransactionSerialiser(transactionSigner(), chainId);

    final HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setDefaultHost(LOCALHOST);
    httpClientOptions.setDefaultPort(clientAndServer.getLocalPort());

    final ServerSocket serverSocket = new ServerSocket(0);
    RestAssured.port = serverSocket.getLocalPort();
    final HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(serverSocket.getLocalPort());
    httpServerOptions.setHost("localhost");

    final Web3j web3j =
        new JsonRpc2_0Web3j(
            new HttpService(
                "http://"
                    + httpClientOptions.getDefaultHost()
                    + ":"
                    + httpClientOptions.getDefaultPort()),
            2000,
            defaultExecutorService());

    runner =
        new Runner(
            serialiser,
            httpClientOptions,
            httpServerOptions,
            downstreamTimeout,
            new Web3jNonceProvider(web3j, serialiser.getAddress()),
            null);
    runner.start();

    LOG.info(
        "Started ethSigner on port {}, eth stub node on port {}",
        serverSocket.getLocalPort(),
        clientAndServer.getLocalPort());
    serverSocket.close();

    unlockedAccount = serialiser.getAddress();
  }

  protected static void resetEthSigner() throws IOException, CipherException {
    setupEthSigner();
  }

  protected Web3j jsonRpc() {
    return jsonRpc;
  }

  @Before
  public void setup() {
    jsonRpc = new JsonRpc2_0Web3j(null, 2000, defaultExecutorService());
    if (clientAndServer.isRunning()) {
      clientAndServer.reset();
      setupTransactionCountResponder();
    }
  }

  private void setupTransactionCountResponder() {
    final EthTransactionCountResponder getTransactionResponse =
        new EthTransactionCountResponder(nonce -> nonce.add(BigInteger.ONE));
    clientAndServer.when(getTransactionResponse.request()).respond(getTransactionResponse);
  }

  @AfterClass
  public static void teardown() {
    clientAndServer.stop();
    runner.stop();
  }

  public void setUpEthNodeResponse(final EthNodeRequest request, final EthNodeResponse response) {
    final List<Header> headers = convertHeadersToMockServerHeaders(response.getHeaders());
    clientAndServer
        .when(request().withBody(json(request.getBody())), exactly(1))
        .respond(
            response()
                .withBody(response.getBody())
                .withHeaders(headers)
                .withStatusCode(response.getStatusCode()));
  }

  public void setupEthNodeResponse(
      final String bodyRegex, final EthNodeResponse response, int count) {
    final List<Header> headers = convertHeadersToMockServerHeaders(response.getHeaders());
    clientAndServer
        .when(request().withBody(new RegexBody(bodyRegex)), exactly(count))
        .respond(
            response()
                .withBody(response.getBody())
                .withHeaders(headers)
                .withStatusCode(response.getStatusCode()));
  }

  public void timeoutRequest(final String bodyRegex) {
    final int ENSURE_TIMEOUT = 5;
    clientAndServer
        .when(request().withBody(new RegexBody(bodyRegex)))
        .respond(
            response()
                .withDelay(TimeUnit.MILLISECONDS, downstreamTimeout.toMillis() + ENSURE_TIMEOUT));
  }

  public void timeoutRequest(final EthNodeRequest request) {
    final int ENSURE_TIMEOUT = 5;
    clientAndServer
        .when(request().withBody(json(request.getBody())), exactly(1))
        .respond(
            response()
                .withDelay(TimeUnit.MILLISECONDS, downstreamTimeout.toMillis() + ENSURE_TIMEOUT));
  }

  public void setUpEthNodeResponse(
      final EthNodeRequest request, final EthNodeResponse response, final Delay delay) {
    final List<Header> headers = convertHeadersToMockServerHeaders(response.getHeaders());
    clientAndServer
        .when(request().withBody(json(request.getBody())), exactly(1))
        .respond(
            response()
                .withBody(response.getBody())
                .withHeaders(headers)
                .withStatusCode(response.getStatusCode())
                .withDelay(delay));
  }

  public void sendRequestThenVerifyResponse(
      final EthSignerRequest request, final EthSignerResponse expectResponse) {
    given()
        .when()
        .body(request.getBody())
        .headers(request.getHeaders())
        .post()
        .then()
        .statusCode(expectResponse.getStatusCode())
        .body(equalTo(expectResponse.getBody()))
        .headers(expectResponse.getHeaders());
  }

  public void verifyEthNodeReceived(final String proxyBodyRequest) {
    clientAndServer.verify(
        request()
            .withBody(proxyBodyRequest)
            .withHeaders(convertHeadersToMockServerHeaders(emptyMap())));
  }

  public void verifyEthNodeReceived(
      final Map<String, String> proxyHeaders, final String proxyBodyRequest) {
    clientAndServer.verify(
        request()
            .withBody(proxyBodyRequest)
            .withHeaders(convertHeadersToMockServerHeaders(proxyHeaders)));
  }

  private List<Header> convertHeadersToMockServerHeaders(final Map<String, String> headers) {
    return headers.entrySet().stream()
        .map(e -> new Header(e.getKey(), e.getValue()))
        .collect(toList());
  }

  private static TransactionSigner transactionSigner() throws IOException, CipherException {
    final File keyFile = createKeyFile();
    return FileBasedTransactionSigner.createFrom(keyFile, "password");
  }

  @SuppressWarnings("UnstableApiUsage")
  private static File createKeyFile() throws IOException {
    final URL walletResource = Resources.getResource("keyfile.json");
    final Path wallet = Files.createTempFile("ethsigner_intg_keyfile", ".json");
    Files.write(wallet, Resources.toString(walletResource, UTF_8).getBytes(UTF_8));
    File keyFile = wallet.toFile();
    keyFile.deleteOnExit();
    return keyFile;
  }

  protected static String generateTransactionCountResponse() {
    final JsonObject json = new JsonObject();
    json.put("id", 1);
    json.put("jsonrpc", "2.0");
    json.put("result", "0x0");

    return json.encode();
  }
}

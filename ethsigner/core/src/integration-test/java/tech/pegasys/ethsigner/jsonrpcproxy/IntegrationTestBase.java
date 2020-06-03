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

import tech.pegasys.ethsigner.core.Runner;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.DownstreamPathCalculator;
import tech.pegasys.ethsigner.jsonrpcproxy.model.request.EthNodeRequest;
import tech.pegasys.ethsigner.jsonrpcproxy.model.request.EthRequestFactory;
import tech.pegasys.ethsigner.jsonrpcproxy.model.request.EthSignerRequest;
import tech.pegasys.ethsigner.jsonrpcproxy.model.response.EthNodeResponse;
import tech.pegasys.ethsigner.jsonrpcproxy.model.response.EthResponseFactory;
import tech.pegasys.ethsigner.jsonrpcproxy.model.response.EthSignerResponse;
import tech.pegasys.signers.secp256k1.api.SingleTransactionSignerProvider;
import tech.pegasys.signers.secp256k1.api.TransactionSigner;
import tech.pegasys.signers.secp256k1.api.TransactionSignerProvider;
import tech.pegasys.signers.secp256k1.filebased.FileBasedSignerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.JsonBody;
import org.mockserver.model.RegexBody;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.eea.Eea;
import org.web3j.protocol.eea.JsonRpc2_0Eea;

public class IntegrationTestBase {

  private static final Logger LOG = LogManager.getLogger();
  private static final String PORTS_FILENAME = "ethsigner.ports";
  private static final String HTTP_JSON_RPC_KEY = "http-jsonrpc";
  private static final String LOCALHOST = "127.0.0.1";
  public static final long DEFAULT_CHAIN_ID = 9;
  public static final int DEFAULT_ID = 77;

  static final String MALFORMED_JSON = "{Bad Json: {{{}";

  private static Vertx vertx;
  private static Runner runner;
  static ClientAndServer clientAndServer;
  static Credentials credentials;

  private JsonRpc2_0Web3j jsonRpc;
  private JsonRpc2_0Eea eeaJsonRpc;

  protected final EthRequestFactory request = new EthRequestFactory();
  protected final EthResponseFactory response = new EthResponseFactory();

  static String unlockedAccount;

  private static final Duration downstreamTimeout = Duration.ofSeconds(1);

  @TempDir static Path dataPath;

  static void setupEthSigner(final long chainId) throws IOException, CipherException {
    setupEthSigner(chainId, "");
  }

  static void setupEthSigner(final long chainId, final String downstreamHttpRequestPath)
      throws IOException, CipherException {
    clientAndServer = startClientAndServer();

    final File keyFile = createKeyFile();
    final File passwordFile = createFile("password");
    credentials = WalletUtils.loadCredentials("password", keyFile);

    final TransactionSignerProvider transactionSignerProvider =
        new SingleTransactionSignerProvider(transactionSigner(keyFile, passwordFile));

    final HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setDefaultHost(LOCALHOST);
    httpClientOptions.setDefaultPort(clientAndServer.getLocalPort());

    final HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(0);
    httpServerOptions.setHost("localhost");

    // Force TransactionDeserialisation to fail
    final ObjectMapper jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    final JsonDecoder jsonDecoder = new JsonDecoder(jsonObjectMapper);

    vertx = Vertx.vertx();
    runner =
        new Runner(
            chainId,
            transactionSignerProvider,
            httpClientOptions,
            httpServerOptions,
            downstreamTimeout,
            new DownstreamPathCalculator(downstreamHttpRequestPath),
            jsonDecoder,
            dataPath,
            vertx);
    runner.start();

    final Path portsFile = dataPath.resolve(PORTS_FILENAME);
    waitForNonEmptyFileToExist(portsFile);
    final int ethSignerPort = httpJsonRpcPort(portsFile);
    RestAssured.port = ethSignerPort;

    LOG.info(
        "Started ethSigner on port {}, eth stub node on port {}",
        ethSignerPort,
        clientAndServer.getLocalPort());

    unlockedAccount =
        transactionSignerProvider.availableAddresses().stream().findAny().orElseThrow();
  }

  Web3j jsonRpc() {
    return jsonRpc;
  }

  Eea eeaJsonRpc() {
    return eeaJsonRpc;
  }

  @BeforeEach
  public void setup() {
    jsonRpc = new JsonRpc2_0Web3j(null, 2000, defaultExecutorService());
    eeaJsonRpc = new JsonRpc2_0Eea(null);
    if (clientAndServer.isRunning()) {
      clientAndServer.reset();
    }
  }

  @AfterAll
  public static void teardown() {
    clientAndServer.stop();
    vertx.close();
    clientAndServer = null;
    runner = null;
  }

  void setUpEthNodeResponse(final EthNodeRequest request, final EthNodeResponse response) {
    final List<Header> headers = convertHeadersToMockServerHeaders(response.getHeaders());
    clientAndServer
        .when(request().withBody(json(request.getBody())), exactly(1))
        .respond(
            response()
                .withBody(response.getBody())
                .withHeaders(headers)
                .withStatusCode(response.getStatusCode()));
  }

  void setupEthNodeResponse(
      final String bodyRegex, final EthNodeResponse response, final int count) {
    final List<Header> headers = convertHeadersToMockServerHeaders(response.getHeaders());
    clientAndServer
        .when(request().withBody(new RegexBody(bodyRegex)), exactly(count))
        .respond(
            response()
                .withBody(response.getBody())
                .withHeaders(headers)
                .withStatusCode(response.getStatusCode()));
  }

  void timeoutRequest(final String bodyRegex) {
    final int ENSURE_TIMEOUT = 5;
    clientAndServer
        .when(request().withBody(new RegexBody(bodyRegex)))
        .respond(
            response()
                .withDelay(TimeUnit.MILLISECONDS, downstreamTimeout.toMillis() + ENSURE_TIMEOUT));
  }

  void timeoutRequest(final EthNodeRequest request) {
    final int ENSURE_TIMEOUT = 5;
    clientAndServer
        .when(request().withBody(json(request.getBody())), exactly(1))
        .respond(
            response()
                .withDelay(TimeUnit.MILLISECONDS, downstreamTimeout.toMillis() + ENSURE_TIMEOUT));
  }

  void sendPostRequestAndVerifyResponse(
      final EthSignerRequest request, final EthSignerResponse expectResponse) {
    sendPostRequestAndVerifyResponse(request, expectResponse, "/");
  }

  void sendPostRequestAndVerifyResponse(
      final EthSignerRequest request, final EthSignerResponse expectResponse, final String path) {
    given()
        .when()
        .body(request.getBody())
        .headers(request.getHeaders())
        .post(path)
        .then()
        .statusCode(expectResponse.getStatusCode())
        .body(equalTo(expectResponse.getBody()))
        .headers(expectResponse.getHeaders());
  }

  void sendPutRequestAndVerifyResponse(
      final EthSignerRequest request, final EthSignerResponse expectResponse, final String path) {
    given()
        .when()
        .body(request.getBody())
        .headers(request.getHeaders())
        .put(path)
        .then()
        .statusCode(expectResponse.getStatusCode())
        .body(equalTo(expectResponse.getBody()))
        .headers(expectResponse.getHeaders());
  }

  void sendGetRequestAndVerifyResponse(
      final EthSignerRequest request, final EthSignerResponse expectResponse, final String path) {
    given()
        .when()
        .body(request.getBody())
        .headers(request.getHeaders())
        .get(path)
        .then()
        .statusCode(expectResponse.getStatusCode())
        .body(equalTo(expectResponse.getBody()))
        .headers(expectResponse.getHeaders());
  }

  void sendDeleteRequestAndVerifyResponse(
      final EthSignerRequest request, final EthSignerResponse expectResponse, final String path) {
    given()
        .when()
        .body(request.getBody())
        .headers(request.getHeaders())
        .delete(path)
        .then()
        .statusCode(expectResponse.getStatusCode())
        .body(equalTo(expectResponse.getBody()))
        .headers(expectResponse.getHeaders());
  }

  void verifyEthNodeReceived(final String proxyBodyRequest) {
    clientAndServer.verify(
        request()
            .withBody(JsonBody.json(proxyBodyRequest))
            .withHeaders(convertHeadersToMockServerHeaders(emptyMap())));
  }

  void verifyEthNodeReceived(final Map<String, String> headers, final String proxyBodyRequest) {
    clientAndServer.verify(
        request()
            .withBody(proxyBodyRequest)
            .withHeaders(convertHeadersToMockServerHeaders(headers)));
  }

  void verifyEthNodeReceived(
      final Map<String, String> headers, final String proxyBodyRequest, final String path) {
    clientAndServer.verify(
        request()
            .withPath(path)
            .withBody(JsonBody.json(proxyBodyRequest))
            .withHeaders(convertHeadersToMockServerHeaders(headers)));
  }

  private List<Header> convertHeadersToMockServerHeaders(final Map<String, String> headers) {
    return headers.entrySet().stream()
        .map((Map.Entry<String, String> e) -> new Header(e.getKey(), e.getValue()))
        .collect(toList());
  }

  private static TransactionSigner transactionSigner(final File keyFile, final File passwordFile) {
    return FileBasedSignerFactory.createSigner(keyFile.toPath(), passwordFile.toPath());
  }

  @SuppressWarnings("UnstableApiUsage")
  private static File createKeyFile() throws IOException {
    final URL walletResource = Resources.getResource("keyfile.json");
    final Path wallet = Files.createTempFile("ethsigner_intg_keyfile", ".json");
    Files.write(wallet, Resources.toString(walletResource, UTF_8).getBytes(UTF_8));
    final File keyFile = wallet.toFile();
    keyFile.deleteOnExit();
    return keyFile;
  }

  private static File createFile(final String s) throws IOException {
    final Path path = Files.createTempFile("file", ".file");
    Files.write(path, s.getBytes(UTF_8));
    final File file = path.toFile();
    file.deleteOnExit();
    return file;
  }

  private static int httpJsonRpcPort(final Path portsFile) {
    try (final FileInputStream fis = new FileInputStream(portsFile.toString())) {
      final Properties portProperties = new Properties();
      portProperties.load(fis);
      final String value = portProperties.getProperty(HTTP_JSON_RPC_KEY);
      return Integer.parseInt(value);
    } catch (final IOException e) {
      throw new RuntimeException("Error reading Web3Provider ports file", e);
    }
  }

  private static void waitForNonEmptyFileToExist(final Path path) {
    final File file = path.toFile();
    Awaitility.waitAtMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              if (file.exists()) {
                try (final Stream<String> s = Files.lines(file.toPath())) {
                  return s.count() > 0;
                }
              }
              return false;
            });
  }
}

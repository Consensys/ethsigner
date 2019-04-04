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
package tech.pegasys.ethfirewall.jsonrpcproxy;

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

import tech.pegasys.ethfirewall.Runner;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.request.EthFirewallRequest;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.request.EthNodeRequest;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.request.EthRequestFactory;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.response.EthFirewallResponse;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.response.EthNodeResponse;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.response.EthResponseFactory;
import tech.pegasys.ethfirewall.signing.ChainIdProvider;
import tech.pegasys.ethfirewall.signing.ConfigurationChainId;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import java.io.File;
import java.io.IOException;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;

public class IntegrationTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestBase.class);
  private static final String LOCALHOST = "127.0.0.1";
  private static final long DEFAULT_CHAIN_ID = 9;

  protected static final String MALFORMED_JSON = "{Bad Json: {{{}";

  private static Runner runner;
  private static ClientAndServer clientAndServer;

  private JsonRpc2_0Web3j jsonRpc;

  protected final EthRequestFactory request = new EthRequestFactory();
  protected final EthResponseFactory response = new EthResponseFactory();

  protected static String unlockedAccount;

  @BeforeClass
  public static void setupEthFirewall() throws IOException, CipherException {
    setupEthFirewall(DEFAULT_CHAIN_ID);
  }

  protected static void setupEthFirewall(final long chainId) throws IOException, CipherException {
    clientAndServer = startClientAndServer();

    final TransactionSigner transactionSigner =
        transactionSigner(new ConfigurationChainId(chainId));

    final HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setDefaultHost(LOCALHOST);
    httpClientOptions.setDefaultPort(clientAndServer.getLocalPort());

    final ServerSocket serverSocket = new ServerSocket(0);
    RestAssured.port = serverSocket.getLocalPort();
    final HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(serverSocket.getLocalPort());
    httpServerOptions.setHost("localhost");

    runner =
        new Runner(transactionSigner, httpClientOptions, httpServerOptions, Duration.ofSeconds(5));
    runner.start();

    LOG.info(
        "Started ethFirewall on port {}, eth stub node on port {}",
        serverSocket.getLocalPort(),
        clientAndServer.getLocalPort());
    serverSocket.close();

    unlockedAccount = transactionSigner.getAddress();
  }

  protected static void resetEthFirewall() throws IOException, CipherException {
    setupEthFirewall();
  }

  protected Web3j jsonRpc() {
    return jsonRpc;
  }

  @Before
  public void setup() {
    jsonRpc = new JsonRpc2_0Web3j(null, 2000, defaultExecutorService());
    clientAndServer.reset();
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

  public void sendRequestThenVerifyResponse(
      final EthFirewallRequest request, final EthFirewallResponse expectResponse) {
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

  private static TransactionSigner transactionSigner(final ChainIdProvider chain)
      throws IOException, CipherException {
    final File keyFile = createKeyFile();
    final Credentials credentials = WalletUtils.loadCredentials("password", keyFile);

    return new TransactionSigner(chain, credentials);
  }

  @SuppressWarnings("UnstableApiUsage")
  private static File createKeyFile() throws IOException {
    final URL walletResource = Resources.getResource("keyfile.json");
    final Path wallet = Files.createTempFile("ethfirewall_intg_keyfile", ".json");
    Files.write(wallet, Resources.toString(walletResource, UTF_8).getBytes(UTF_8));
    File keyFile = wallet.toFile();
    keyFile.deleteOnExit();
    return keyFile;
  }
}

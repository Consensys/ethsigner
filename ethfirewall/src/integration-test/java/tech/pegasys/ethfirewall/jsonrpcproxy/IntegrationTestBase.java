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
import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
import static org.web3j.utils.Async.defaultExecutorService;

import tech.pegasys.ethfirewall.Runner;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

public class IntegrationTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestBase.class);

  private static final String LOCALHOST = "127.0.0.1";
  private static Runner runner;
  private static ClientAndServer ethNode;
  private ObjectMapper objectMapper = new ObjectMapper();
  JsonRpc2_0Web3j jsonRpc;

  @BeforeClass
  public static void setupEthFirewall() throws IOException, CipherException {
    ethNode = startClientAndServer();

    final File keyFile = createKeyFile();
    final TransactionSigner transactionSigner = TransactionSigner.createFrom(keyFile, "password");

    final HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setDefaultHost(LOCALHOST);
    httpClientOptions.setDefaultPort(ethNode.getLocalPort());

    final ServerSocket serverSocket = new ServerSocket(0);
    RestAssured.port = serverSocket.getLocalPort();
    final HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(serverSocket.getLocalPort());
    httpServerOptions.setHost("localhost");

    runner = new Runner(transactionSigner, httpClientOptions, httpServerOptions);
    runner.start();

    LOG.info(
        "Started ethFirewall on port {}, eth stub node on port {}",
        serverSocket.getLocalPort(),
        ethNode.getLocalPort());
    serverSocket.close();
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

  @Before
  public void setup() {
    jsonRpc = new JsonRpc2_0Web3j(null, 2000, defaultExecutorService());
    ethNode.reset();
  }

  @AfterClass
  public static void teardown() {
    ethNode.stop();
    runner.stop();
  }

  public void configureEthNode(
      final Request<?, ? extends Response> request,
      final Object response,
      final Map<String, String> responseHeaders,
      final int responseStatusCode)
      throws JsonProcessingException {
    final String requestBody = objectMapper.writeValueAsString(request);
    final String responseBody = objectMapper.writeValueAsString(response);
    List<Header> headers = convertHeadersToMockServerHeaders(responseHeaders);
    ethNode
        .when(request().withBody(json(requestBody)), exactly(1))
        .respond(
            response()
                .withBody(responseBody)
                .withHeaders(headers)
                .withStatusCode(responseStatusCode));
  }

  public void sendRequestAndVerify(
      final Request<?, ? extends Response> proxyBodyRequest,
      final Map<String, String> proxyHeaders,
      final Object response,
      final int ethNodeStatusCode,
      final Map<String, String> ethNodeHeaders)
      throws JsonProcessingException {
    String responseBody = objectMapper.writeValueAsString(response);
    given()
        .when()
        .body(proxyBodyRequest)
        .headers(proxyHeaders)
        .post()
        .then()
        .statusCode(ethNodeStatusCode)
        .body(equalTo(responseBody))
        .headers(ethNodeHeaders);
  }

  public <T extends Response> void verifyEthNodeRequest(
      final Request<?, T> proxyBodyRequest, final Map<String, String> proxyHeaders) {
    ethNode.verify(
        request()
            .withBody(json(proxyBodyRequest))
            .withHeaders(convertHeadersToMockServerHeaders(proxyHeaders)));
  }

  private List<Header> convertHeadersToMockServerHeaders(final Map<String, String> headers) {
    return headers.entrySet().stream()
        .map(e -> new Header(e.getKey(), e.getValue()))
        .collect(toList());
  }
}

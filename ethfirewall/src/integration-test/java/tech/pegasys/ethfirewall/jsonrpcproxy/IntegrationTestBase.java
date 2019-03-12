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
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import tech.pegasys.ethfirewall.Runner;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.web3j.crypto.CipherException;

public class IntegrationTestBase {
  static ClientAndServer ethNode;
  private static Runner runner;

  @BeforeClass
  public static void setup() throws IOException, CipherException {
    ethNode = startClientAndServer();

    final File keyFile = createKeyFile();
    final TransactionSigner transactionSigner = TransactionSigner.createFrom(keyFile, "password");

    final HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setDefaultHost("localhost");
    httpClientOptions.setDefaultPort(ethNode.getLocalPort());

    final ServerSocket serverSocket = new ServerSocket(0);
    RestAssured.port = serverSocket.getLocalPort();
    final HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(serverSocket.getLocalPort());
    httpServerOptions.setHost("localhost");

    runner = new Runner(transactionSigner, httpClientOptions, httpServerOptions);
    runner.start();
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
  public void resetEthNode() {
    ethNode.reset();
  }

  @AfterClass
  public static void teardown() {
    ethNode.stop();
    runner.stop();
  }

  public void configureEthNode(
      final String method, final String responseBody, final Map<String, String> responseHeaders) {
    List<Header> headers = convertHeadersToMockServerHeaders(responseHeaders);
    ethNode
        .when(
            request()
                .withMethod("POST")
                .withPath("/")
                .withBody(json("{\"method\":\"" + method + "\"}")),
            Times.exactly(1))
        .respond(response().withBody(responseBody).withHeaders(headers));
  }

  public void sendRequest(final String proxyBodyRequest, final Map<String, String> proxyHeaders,
      final String ethNodeBody, final int ethNodeStatusCode,
      final Map<String, String> ethNodeHeaders) {
    given()
        .when()
        .body(proxyBodyRequest)
        .headers(proxyHeaders)
        .post()
        .then()
        .statusCode(ethNodeStatusCode)
        .body(equalTo(ethNodeBody))
        .headers(ethNodeHeaders);
  }

  public void verifyEthNodeRequest(final String proxyBodyRequest,
      final Map<String, String> proxyHeaders) {
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

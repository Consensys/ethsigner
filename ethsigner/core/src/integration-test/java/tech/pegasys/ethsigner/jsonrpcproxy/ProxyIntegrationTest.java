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

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.NetVersion;

public class ProxyIntegrationTest extends IntegrationTestBase {
  private static final String LOGIN_BODY = "{\"username\":\"username1\",\"password\":\"pegasys\"}";
  private static final String LOGIN_RESPONSE = "{\"token\":\"eyJ0\"}";
  private static final Map<String, String> REQUEST_HEADERS = singletonMap("Accept", "*/*");
  private static final Map<String, String> RESPONSE_HEADERS =
      singletonMap("Content-Type", "Application/Json");

  private static final String ROOT_PATH = "/arbitraryRootPath";

  @BeforeAll
  public static void localSetup() {
    try {
      setupEthSigner(DEFAULT_CHAIN_ID, ROOT_PATH);
    } catch (final CipherException | IOException e) {
      throw new RuntimeException("Failed to setup ethsigner");
    }
  }

  @Test
  void requestWithHeadersIsProxied() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());

    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);

    setUpEthNodeResponse(
        request.ethNode(netVersionRequest), response.ethNode(RESPONSE_HEADERS, netVersionResponse));

    sendPostRequestAndVerifyResponse(
        request.ethSigner(REQUEST_HEADERS, netVersionRequest),
        response.ethSigner(RESPONSE_HEADERS, netVersionResponse));

    verifyEthNodeReceived(REQUEST_HEADERS, netVersionRequest);
  }

  @Test
  void requestReturningErrorIsProxied() {
    final String ethProtocolVersionRequest = Json.encode(jsonRpc().ethProtocolVersion());

    setUpEthNodeResponse(
        request.ethNode(ethProtocolVersionRequest),
        response.ethNode("Not Found", HttpResponseStatus.NOT_FOUND));

    sendPostRequestAndVerifyResponse(
        request.ethSigner(ethProtocolVersionRequest),
        response.ethSigner("Not Found", HttpResponseStatus.NOT_FOUND));

    verifyEthNodeReceived(ethProtocolVersionRequest);
  }

  @Test
  void postRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    sendPostRequestAndVerifyResponse(
        request.ethSigner(REQUEST_HEADERS, LOGIN_BODY),
        response.ethSigner(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }

  @Test
  void getRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    // Whilst a get request doesn't normally have a body, it can and we want to ensure the request
    // is proxied as is
    sendGetRequestAndVerifyResponse(
        request.ethSigner(REQUEST_HEADERS, LOGIN_BODY),
        response.ethSigner(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }

  @Test
  void putRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    sendPutRequestAndVerifyResponse(
        request.ethSigner(REQUEST_HEADERS, LOGIN_BODY),
        response.ethSigner(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }

  @Test
  void deleteRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    sendDeleteRequestAndVerifyResponse(
        request.ethSigner(REQUEST_HEADERS, LOGIN_BODY),
        response.ethSigner(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }
}

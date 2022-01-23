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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.model.Header;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.NetVersion;

public class AllDomainsCorsIntegrationTest extends IntegrationTestBase {

  private static final Iterable<Entry<String, String>> RESPONSE_HEADERS =
      singletonList(ImmutablePair.of("Content-Type", "Application/Json"));

  private static final String ROOT_PATH = "/arbitraryRootPath";

  @BeforeAll
  public static void localSetup() {
    try {
      setupEthSigner(DEFAULT_CHAIN_ID, ROOT_PATH, List.of("*"));
    } catch (final Exception e) {
      throw new RuntimeException("Failed to setup ethsigner");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"moz-extension://802123e4-a916-2d4e-bebf-384b0e2e86dd", "sample.com"})
  void requestFromVariousOriginTypesShouldSucceedWhenCorsIsStar(final String originDomain) {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());
    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);

    setUpEthNodeResponse(
        request.ethNode(netVersionRequest), response.ethNode(RESPONSE_HEADERS, netVersionResponse));

    final List<Entry<String, String>> expectedResponseHeaders =
        Lists.newArrayList(RESPONSE_HEADERS);
    expectedResponseHeaders.add(
        ImmutablePair.of(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), originDomain));

    final Iterable<Entry<String, String>> requestHeaders =
        List.of(
            ImmutablePair.of("Accept", "*/*"),
            ImmutablePair.of("Host", "localhost"),
            ImmutablePair.of("Origin", originDomain));

    sendPostRequestAndVerifyResponse(
        request.ethSigner(requestHeaders, netVersionRequest),
        response.ethSigner(expectedResponseHeaders, netVersionResponse));

    // Cors headers should not be forwarded to the downstream web3 provider (CORS is handled
    // entirely within Ethsigner.
    assertThat(clientAndServer.retrieveRecordedRequests(request().withHeader(new Header("origin"))))
        .isEmpty();
  }
}

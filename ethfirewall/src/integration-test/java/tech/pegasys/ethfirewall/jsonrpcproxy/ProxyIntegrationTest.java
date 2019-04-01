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

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.junit.Test;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.NetVersion;

public class ProxyIntegrationTest extends IntegrationTestBase {

  @Test
  public void requestWithHeadersIsProxied() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());
    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);
    final Map<String, String> requestHeaders = ImmutableMap.of("Accept", "*/*");
    final Map<String, String> responseHeaders = ImmutableMap.of("Content-Type", "Application/Json");
    setUpEthNodeResponse(
        ethNode.request(netVersionRequest), ethNode.response(responseHeaders, netVersionResponse));

    sendResponseThenVerify(
        ethFirewall.request(requestHeaders, netVersionRequest),
        ethFirewall.response(responseHeaders, netVersionResponse));

    verifyEthNodeReceived(requestHeaders, netVersionRequest);
  }

  @Test
  public void requestReturningErrorIsProxied() {
    final String ethProtocolVersionRequest = Json.encode(jsonRpc().ethProtocolVersion());

    setUpEthNodeResponse(
        ethNode.request(ethProtocolVersionRequest),
        ethNode.response("Not Found", HttpResponseStatus.NOT_FOUND));

    sendResponseThenVerify(
        ethFirewall.request(ethProtocolVersionRequest),
        ethFirewall.response("Not Found", HttpResponseStatus.NOT_FOUND));

    verifyEthNodeReceived(ethProtocolVersionRequest);
  }
}

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

import static java.util.Collections.emptyMap;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthProtocolVersion;
import org.web3j.protocol.core.methods.response.NetVersion;

public class ProxyIntegrationTest extends IntegrationTestBase {

  @Test
  public void requestWithHeadersIsProxied() {
    final Request<?, NetVersion> netVersionRequest = jsonRpc().netVersion();
    final Response<String> netVersionResponse = new NetVersion();
    netVersionResponse.setResult("4");

    final Map<String, String> requestHeaders = ImmutableMap.of("Accept", "*/*");
    final Map<String, String> responseHeaders = ImmutableMap.of("Content-Type", "Application/Json");

    setUpEthNodeResponse(
        netVersionRequest, netVersionResponse, responseHeaders, HttpResponseStatus.OK);
    sendVerifyingResponse(
        netVersionRequest,
        requestHeaders,
        netVersionResponse,
        HttpResponseStatus.OK,
        responseHeaders);
    verifyEthereumNodeReceived(netVersionRequest, requestHeaders);
  }

  @Test
  public void requestReturningErrorIsProxied() {
    final Request<?, EthProtocolVersion> ethProtocolVersionRequest = jsonRpc().ethProtocolVersion();
    setUpEthNodeResponse(
        ethProtocolVersionRequest, "Not Found", emptyMap(), HttpResponseStatus.NOT_FOUND);
    sendVerifyingResponse(
        ethProtocolVersionRequest,
        emptyMap(),
        "Not Found",
        HttpResponseStatus.NOT_FOUND,
        emptyMap());
    verifyEthereumNodeReceived(ethProtocolVersionRequest);
  }
}

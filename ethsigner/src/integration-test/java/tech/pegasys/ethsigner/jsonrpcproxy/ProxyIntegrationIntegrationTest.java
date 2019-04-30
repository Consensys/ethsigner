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

import static java.util.concurrent.TimeUnit.SECONDS;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.INVALID_REQUEST;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.junit.Test;
import org.mockserver.model.Delay;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.NetVersion;

public class ProxyIntegrationIntegrationTest extends IntegrationTestBase {

  @Test
  public void requestWithHeadersIsProxied() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());
    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);
    final Map<String, String> requestHeaders = ImmutableMap.of("Accept", "*/*");
    final Map<String, String> responseHeaders = ImmutableMap.of("Content-Type", "Application/Json");
    setUpEthNodeResponse(
        request.ethNode(netVersionRequest), response.ethNode(responseHeaders, netVersionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(requestHeaders, netVersionRequest),
        response.ethSigner(responseHeaders, netVersionResponse));

    verifyEthNodeReceived(requestHeaders, netVersionRequest);
  }

  @Test
  public void requestReturningErrorIsProxied() {
    final String ethProtocolVersionRequest = Json.encode(jsonRpc().ethProtocolVersion());

    setUpEthNodeResponse(
        request.ethNode(ethProtocolVersionRequest),
        response.ethNode("Not Found", HttpResponseStatus.NOT_FOUND));

    sendRequestThenVerifyResponse(
        request.ethSigner(ethProtocolVersionRequest),
        response.ethSigner("Not Found", HttpResponseStatus.NOT_FOUND));

    verifyEthNodeReceived(ethProtocolVersionRequest);
  }

  @Test
  public void requestTimingOutReturnsGatewayTimeoutError() {
    final String ethProtocolVersionRequest = Json.encode(jsonRpc().ethProtocolVersion());
    final String jsonRpcErrorResponse = Json.encode(new JsonRpcErrorResponse(0, INTERNAL_ERROR));

    setUpEthNodeResponse(
        request.ethNode(ethProtocolVersionRequest),
        response.ethNode(INVALID_REQUEST),
        new Delay(SECONDS, 2));

    sendRequestThenVerifyResponse(
        request.ethSigner(ethProtocolVersionRequest),
        response.ethSigner(jsonRpcErrorResponse, HttpResponseStatus.GATEWAY_TIMEOUT));

    verifyEthNodeReceived(ethProtocolVersionRequest);
  }
}

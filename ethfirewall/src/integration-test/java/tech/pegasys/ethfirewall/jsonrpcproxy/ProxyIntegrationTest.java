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

import java.math.BigInteger;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthProtocolVersion;

public class ProxyIntegrationTest extends IntegrationTestBase {

  @Test
  public void requestWithHeadersIsProxied() throws Exception {
    final Request<?, ? extends Response> netVersionRequest = jsonRpc.netVersion();
    final Response<String> netVersionResponse = createStringResponse("4");

    final Map<String, String> requestHeaders = ImmutableMap.of("Accept", "*/*");
    final Map<String, String> responseHeaders = ImmutableMap.of("Content-Type", "Application/Json");

    configureEthNode(netVersionRequest, netVersionResponse, responseHeaders, 200);
    sendRequestAndVerify(
        netVersionRequest, requestHeaders, netVersionResponse, 200, responseHeaders);
    verifyEthNodeRequest(netVersionRequest, requestHeaders);
  }

  @Test
  public void requestReturningErrorIsProxied() throws Exception {
    final Request<?, EthProtocolVersion> ethProtocolVersionRequest = jsonRpc.ethProtocolVersion();
    configureEthNode(ethProtocolVersionRequest, "Not Found", emptyMap(), 404);
    sendRequestAndVerify(ethProtocolVersionRequest, emptyMap(), "Not Found", 404, emptyMap());
    verifyEthNodeRequest(ethProtocolVersionRequest, emptyMap());
  }

  @Test
  public void requestWithSendTransactionIsSignedBeforeProxying() throws Exception {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");
    final Request<?, ? extends Response> ethSendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    ethSendTransactionRequest.setId(1);

    Request<?, ? extends Response> ethSendRawTransactionRequest =
        jsonRpc.ethSendRawTransaction(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2"
                + "8609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5"
                + "d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f0724456751ca03631b1cec6"
                + "e5033e8a2bff6d1b2d08bfe106cbbb82df5eb7b380a1fdb5c06be2a06d15eeb833f26114de087c930e375"
                + "56d93a47d86e6554e988d32cbbb273cfda4");
    ethSendRawTransactionRequest.setId(1);

    Response<String> ethSendRawTransactionResponse =
        createStringResponse("0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331");

    configureEthNode(ethSendRawTransactionRequest, ethSendRawTransactionResponse, emptyMap(), 200);
    sendRequestAndVerify(
        ethSendTransactionRequest, emptyMap(), ethSendRawTransactionResponse, 200, emptyMap());
    verifyEthNodeRequest(ethSendRawTransactionRequest, emptyMap());
  }

  private Response<String> createStringResponse(final String result) {
    final Response<String> response = new Response<>();
    response.setId(1);
    response.setResult(result);
    response.setJsonrpc("2.0");
    return response;
  }
}

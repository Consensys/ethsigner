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

import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.NONCE_TOO_LOW;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthProtocolVersion;
import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendRawTransaction;

public class FailedConnectionTest extends IntegrationTestBase {

  @Test
  public void failsToConnectToDownStreamRaisesTimeout() {
    clientAndServer.stop();
    final Request<?, EthProtocolVersion> jsonRpcRequest = jsonRpc().ethProtocolVersion();
    final String ethProtocolVersionRequest = Json.encode(jsonRpcRequest);

    final String expectedResponse =
        Json.encode(
            new JsonRpcErrorResponse(
                jsonRpcRequest.getId(), JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT));

    sendRequestThenVerifyResponse(
        request.ethSigner(ethProtocolVersionRequest),
        response.ethSigner(expectedResponse, HttpResponseStatus.GATEWAY_TIMEOUT));
  }

  @Test
  public void failingToConnectWithNoNonceRaisesTimeout() {
    // Note: This test ensures the behaviour when requesting a nonce as part of the
    // send transaction (performed via web3j) behaves the same as a normal timeout.
    private SendRawTransaction sendRawTransaction = new SendRawTransaction(jsonRpc());
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(
            "0xf892808609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0eab405b58d6aa7db96ebfab8c55825504090447d6209848eeca5a2a2ff909467a064712627fdf02027521a716b8e9a497d31f7c4d5ecb75840fde86ade1d726fab");

    sendRequestThenVerifyResponse(
        request.ethSigner(ethProtocolVersionRequest),
        response.ethSigner(expectedResponse, HttpResponseStatus.GATEWAY_TIMEOUT));  }
}

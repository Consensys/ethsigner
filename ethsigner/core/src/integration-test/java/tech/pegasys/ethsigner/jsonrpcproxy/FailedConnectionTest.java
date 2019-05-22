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

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.junit.Before;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthProtocolVersion;

public class FailedConnectionTest extends IntegrationTestBase {

  @Before
  public void localSetup() {
    clientAndServer.stop();
  }

  @Test
  public void failsToConnectToDownStreamRaisesTimeout() {

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
    final SendTransaction sendTransaction = new SendTransaction(jsonRpc());

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()),
        response.ethSigner(
            JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT,
            HttpResponseStatus.GATEWAY_TIMEOUT));
  }
}

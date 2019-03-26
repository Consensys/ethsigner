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

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

/** Signing is a step during proxying a sendTransaction() JSON-RPC request to an Ethereum node. */
public class SigningSendTransactionTest extends IntegrationTestBase {

  // TODO bad json

  // TODO bad input

  // TODO optional input

  // TODO happy path - all populated

  @Test
  public void signSendTransaction() throws Exception {
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
    final Request<?, ? extends Response<?>> ethSendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    ethSendTransactionRequest.setId(5);

    Request<?, ? extends Response<?>> ethSendRawTransactionRequest =
        jsonRpc.ethSendRawTransaction(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    // we create the eth_sendRawTransaction req with same id as the eth_sendTransaction req
    ethSendRawTransactionRequest.setId(5);

    Response<String> ethSendRawTransactionResponse = new EthSendTransaction();
    ethSendRawTransactionResponse.setResult(
        "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331");

    setUpEthereumNode(
        ethSendRawTransactionRequest,
        ethSendRawTransactionResponse,
        emptyMap(),
        HttpResponseStatus.OK);
    sendRequestAndVerify(
        ethSendTransactionRequest,
        emptyMap(),
        ethSendRawTransactionResponse,
        HttpResponseStatus.OK,
        emptyMap());
    verifyEthNodeRequest(ethSendRawTransactionRequest, emptyMap());
  }
}

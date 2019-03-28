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

import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.EthFirewallRequest;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.EthFirewallResponse;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.EthNodeRequest;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.EthNodeResponse;

import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

/** Signing is a step during proxying a sendTransaction() JSON-RPC request to an Ethereum node. */
public class SigningSendTransactionTest extends IntegrationTestBase {

  private static final Map<String, String> NO_HEADERS = emptyMap();
  private static final String MALFORMED_JSON = "{Bad Json: {{{}";
  private static final Object NO_ID = null;

  @Test
  public void malformedJsonRequest() {
    sendVerifyingResponse(
        ethFirewallRequest(MALFORMED_JSON), ethFirewallResponse(NO_ID, JsonRpcError.PARSE_ERROR));
  }

  @Test
  public void malformedJsonResponse() {
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        defaultSendRawTransactionRequest();
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(MALFORMED_JSON));

    sendVerifyingResponse(
        ethFirewallRequest(sendRawTransactionRequest), ethFirewallResponse(MALFORMED_JSON));
  }

  @Test
  public void invalidNonce() {
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestWithNonce("I'm an invalid nonce!")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidSenderAddressTooShort() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f0723315")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidSenderAddressTooLong() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f07233155A")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidSenderAddressMalformedHex() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidSenderAddressEmpty() {
    final String firewallRequest = defaultSendTransactionRequestWithSender("");

    final Request<?, ? extends Response<?>> nodeRequest =
        sendRawTransactionRequest(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c0940000000000000000000000000000000000000000849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a09d99057d1cb7a52c62c6e81ebf0e14516c5e93812f9a91beaa4576b05242ced4a04a87eefa7aa1240da54d0809f2867526cb726d93c064154a9855c30be6b190e8");

    final Response<String> nodeResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331");

    setUpEthNodeResponse(ehtNodeRequest(nodeRequest), ethNodeResponse(nodeResponse));

    sendVerifyingResponse(ethFirewallRequest(firewallRequest), ethFirewallResponse(nodeResponse));

    verifyEthereumNodeReceived(nodeRequest);
  }

  @Test
  public void missingSenderAddress() {
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestNoSender()),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  // TODO invalid to (invalid hex, out of range)
  // TODO invalid gas (NaN)
  // TODO gas price (NaN)
  // TODO value (NaN)
  // TODO data (missing)

  // TODO optional input

  // TODO happy path - all populated

  // TODO change the chainID when signing

  @Test
  public void signSendTransaction() {

    final Request<?, ? extends Response<?>> firewallRequest = defaultSendTransactionRequest();

    final Request<?, ? extends Response<?>> nodeRequest = defaultSendRawTransactionRequest();

    final Response<String> nodeResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331");

    setUpEthNodeResponse(ehtNodeRequest(nodeRequest), ethNodeResponse(nodeResponse));

    sendVerifyingResponse(ethFirewallRequest(firewallRequest), ethFirewallResponse(nodeResponse));

    verifyEthereumNodeReceived(nodeRequest);
  }

  // TODO refactor below methods into utility (after complete tests)
  private static final int DEFAULT_ID = 77;

  /**
   * Due to the underlying server mocking, When only a single request is used, the contents does not
   * actually matter, only their equivalence does.
   */
  private Request<?, ? extends Response<?>> defaultSendTransactionRequest() {
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

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc().ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return sendTransactionRequest;
  }

  private Request<?, ? extends Response<?>> defaultSendTransactionRequestNoSender() {
    final Transaction transaction =
        new Transaction(
            null,
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc().ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return sendTransactionRequest;
  }

  private String defaultSendTransactionRequestWithNonce(final String nonce) {
    final String sendTransaction = Json.encode(defaultSendTransactionRequest());
    final Pattern nonceWithValue = Pattern.compile("nonce\\\":\\\"(\\w*)\\\"");
    final Matcher matches = nonceWithValue.matcher(sendTransaction);
    return matches.replaceFirst(String.format("nonce\":\"%s\"", nonce));
  }

  private String defaultSendTransactionRequestWithSender(final String sender) {
    final String sendTransaction = Json.encode(defaultSendTransactionRequest());
    final Pattern nonceWithValue = Pattern.compile("to\\\":\\\"(\\w*)\\\"");
    final Matcher matches = nonceWithValue.matcher(sendTransaction);
    return matches.replaceFirst(String.format("to\":\"%s\"", sender));
  }

  private Request<?, ? extends Response<?>> defaultSendRawTransactionRequest() {
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        jsonRpc()
            .ethSendRawTransaction(
                "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    sendRawTransactionRequest.setId(77);

    return sendRawTransactionRequest;
  }

  private Request<?, ? extends Response<?>> sendRawTransactionRequest(final String value) {
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        jsonRpc().ethSendRawTransaction(value);
    sendRawTransactionRequest.setId(77);

    return sendRawTransactionRequest;
  }

  private Response<String> sendRawTransactionResponse(final String value) {
    final Response<String> sendRawTransactionResponse = new EthSendTransaction();
    sendRawTransactionResponse.setResult(value);
    return sendRawTransactionResponse;
  }

  private EthNodeResponse ethNodeResponse(final String body) {
    return new EthNodeResponse(NO_HEADERS, body, HttpResponseStatus.OK);
  }

  private EthNodeRequest ehtNodeRequest(final Request<?, ? extends Response<?>> body) {
    return new EthNodeRequest(NO_HEADERS, body);
  }

  private EthFirewallRequest ethFirewallRequest(final Request<?, ? extends Response<?>> body) {
    return new EthFirewallRequest(NO_HEADERS, Json.encode(body));
  }

  private EthFirewallRequest ethFirewallRequest(final String body) {
    return new EthFirewallRequest(NO_HEADERS, body);
  }

  private EthFirewallResponse ethFirewallResponse(final Object id, final JsonRpcError error) {
    return new EthFirewallResponse(
        NO_HEADERS, new JsonRpcErrorResponse(id, error), HttpResponseStatus.BAD_REQUEST);
  }

  private EthFirewallResponse ethFirewallResponse(final JsonRpcError error) {
    return new EthFirewallResponse(
        NO_HEADERS, new JsonRpcErrorResponse(DEFAULT_ID, error), HttpResponseStatus.BAD_REQUEST);
  }

  private EthFirewallResponse ethFirewallResponse(final Response<String> body) {
    return new EthFirewallResponse(NO_HEADERS, Json.encode(body), HttpResponseStatus.OK);
  }

  private EthFirewallResponse ethFirewallResponse(final String body) {
    return new EthFirewallResponse(NO_HEADERS, body, HttpResponseStatus.OK);
  }

  private EthNodeResponse ethNodeResponse(final Response<String> body) {
    return new EthNodeResponse(NO_HEADERS, Json.encode(body), HttpResponseStatus.OK);
  }
}

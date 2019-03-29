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
  public void parseErrorResponseWhenJsonRequestIsMalformed() {
    sendVerifyingResponse(
        ethFirewallRequest(MALFORMED_JSON), ethFirewallResponse(NO_ID, JsonRpcError.PARSE_ERROR));
  }

  @Test
  public void proxyMalformedJsonResponseFromNode() {
    final Request<?, ? extends Response<?>> sendRawTransaction = defaultSendRawTransactionRequest();
    setUpEthNodeResponse(ehtNodeRequest(sendRawTransaction), ethNodeResponse(MALFORMED_JSON));

    sendVerifyingResponse(
        ethFirewallRequest(sendRawTransaction), ethFirewallResponse(MALFORMED_JSON));
  }

  @Test
  public void invalidParamsResponseWhenNonceIsNaN() {
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestWithNonce("I'm an invalid nonce format!")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void internalErrorResponseWhenMissingNonce() {
    // TODO This behaviour will change with the get nonce (PIE-1468)
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestNoNonce()),
        ethFirewallResponse(JsonRpcError.INTERNAL_ERROR));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooShort() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f0723315")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooLong() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f07233155A")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsMalformedHex() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenSenderAddressIsEmpty() {
    final String sendTransactionRequest = defaultSendTransactionRequestWithSender("");
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c0940000000000000000000000000000000000000000849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a09d99057d1cb7a52c62c6e81ebf0e14516c5e93812f9a91beaa4576b05242ced4a04a87eefa7aa1240da54d0809f2867526cb726d93c064154a9855c30be6b190e8");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527333");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenMissingSenderAddress() {
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestNoSender()),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenReceiverAddressIsTooShort() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithReceiver("0xb60e8dd61c5d32be8058bb8eb970870f0723315")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenReceiverAddressIsTooLong() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithReceiver(
                "0xb60e8dd61c5d32be8058bb8eb970870f07233155A")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidParamsWhenReceiverAddressIsMalformedHex() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithReceiver(
                "0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenReceiverAddressIsEmpty() {
    final String sendTransactionRequest = defaultSendTransactionRequestWithReceiver("");
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1524444");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signTransactionWhenMissingReceiverAddress() {
    final Request<?, ? extends Response<?>> sendTransactionRequest =
        defaultSendTransactionRequestNoReceiver();
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf89ea0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c080849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a09667b2df27d9bed3df507cbe4a0df47038934c350e442349546bedff0ebbe005a077d738b7c379683114694e98ddff0930a03ba1693fbb8ae597afc689757d9c6d");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1355555");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signTransactionWhenMissingValue() {
    final Request<?, ? extends Response<?>> sendTransactionRequest =
        defaultSendTransactionRequestNoValue();
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8aea0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a09909c15a400a8ee08025883c3f136287f2129b48cd5f3ebfae5f344c0deeb0e9a0382f174debaaee54054c9a2cb0ddc71cde22da321e83c940f34b24c1291236b9");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1666666");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenValueIsNaN() {
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestWithValue("I'm an invalid value format!")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGas() {
    final Request<?, ? extends Response<?>> sendTransactionRequest =
        defaultSendTransactionRequestNoGas();
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8aca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2808276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0e22863d8ad64de06fe67f5d22eb19bf90ef26cb089a4912f846b42b10fa6027aa019b9851d46f1f5098b30a68133755d14e9bb43b86de750d9363b0cb7e1d6f939");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d7777777");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasIsNaN() {
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestWithGas("I'm an invalid gas format!")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGasPrice() {
    final Request<?, ? extends Response<?>> sendTransactionRequest =
        defaultSendTransactionRequestNoGasPrice();
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b3a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a00083015f9094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0f6950faa587a4e12ac799ed27b247384684c4ea35274708fc25095a82df4abe0a02d13f3b28eff9a51eeb3a8e97975c43f41d79ec704bbfd9519be4bc27a255fcc");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasPriceIsNaN() {
    sendVerifyingResponse(
        ethFirewallRequest(
            defaultSendTransactionRequestWithGasPrice("I'm an invalid gas price format!")),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenMissingData() {
    sendVerifyingResponse(
        ethFirewallRequest(defaultSendTransactionRequestNoData()),
        ethFirewallResponse(JsonRpcError.INVALID_PARAMS));
  }

  @Test
  public void signSendTransactionWhenContract() {
    final Request<?, ? extends Response<?>> sendTransactionRequest =
        smartContractSendTransactionRequest();
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf90182018083015f908080b90134608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a35a0d23d332cad1010a308bf531ccbb8985349eedfe9ec823b34c6e7fbddcba02420a048c9496343c72828e15dac889dfbb2aa5d9b2c23e87357e571350375aef90651");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signSendTransaction() {
    final Request<?, ? extends Response<?>> sendTransactionRequest =
        defaultSendTransactionRequest();
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    final Response<String> sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102999999999");
    setUpEthNodeResponse(
        ehtNodeRequest(sendRawTransactionRequest), ethNodeResponse(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewallRequest(sendTransactionRequest),
        ethFirewallResponse(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  // TODO integer values, not wrapped as strings

  // TODO change the chainID when signing

  // TODO ----- refactor below methods into utility (after complete tests) ----
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

  private Request<?, ? extends Response<?>> smartContractSendTransactionRequest() {
    final Transaction transaction =
        new Transaction(
            "0xae8ed09c458cebc142c06bdd297709575482b0fd",
            new BigInteger("1"),
            null,
            null,
            null,
            null,
            "0x608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a");

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

  private Request<?, ? extends Response<?>> defaultSendTransactionRequestNoNonce() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            null,
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

  private Request<?, ? extends Response<?>> defaultSendTransactionRequestNoReceiver() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            null,
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc().ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return sendTransactionRequest;
  }

  private Request<?, ? extends Response<?>> defaultSendTransactionRequestNoValue() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            null,
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc().ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return sendTransactionRequest;
  }

  private Request<?, ? extends Response<?>> defaultSendTransactionRequestNoGas() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            null,
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc().ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return sendTransactionRequest;
  }

  private Request<?, ? extends Response<?>> defaultSendTransactionRequestNoGasPrice() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            null,
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc().ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return sendTransactionRequest;
  }

  private Request<?, ? extends Response<?>> defaultSendTransactionRequestNoData() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            null);

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc().ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return sendTransactionRequest;
  }

  private String defaultSendTransactionRequestWithGas(final String value) {
    final String sendTransaction = Json.encode(defaultSendTransactionRequest());
    final Pattern nonceWithValue = Pattern.compile("gas\\\":\\\"(\\w*)\\\"");
    final Matcher matches = nonceWithValue.matcher(sendTransaction);
    return matches.replaceFirst(String.format("gas\":\"%s\"", value));
  }

  private String defaultSendTransactionRequestWithGasPrice(final String value) {
    final String sendTransaction = Json.encode(defaultSendTransactionRequest());
    final Pattern nonceWithValue = Pattern.compile("gasPrice\\\":\\\"(\\w*)\\\"");
    final Matcher matches = nonceWithValue.matcher(sendTransaction);
    return matches.replaceFirst(String.format("gasPrice\":\"%s\"", value));
  }

  private String defaultSendTransactionRequestWithValue(final String value) {
    final String sendTransaction = Json.encode(defaultSendTransactionRequest());
    final Pattern nonceWithValue = Pattern.compile("value\\\":\\\"(\\w*)\\\"");
    final Matcher matches = nonceWithValue.matcher(sendTransaction);
    return matches.replaceFirst(String.format("value\":\"%s\"", value));
  }

  private String defaultSendTransactionRequestWithNonce(final String nonce) {
    final String sendTransaction = Json.encode(defaultSendTransactionRequest());
    final Pattern nonceWithValue = Pattern.compile("nonce\\\":\\\"(\\w*)\\\"");
    final Matcher matches = nonceWithValue.matcher(sendTransaction);
    return matches.replaceFirst(String.format("nonce\":\"%s\"", nonce));
  }

  private String defaultSendTransactionRequestWithReceiver(final String sender) {
    final String sendTransaction = Json.encode(defaultSendTransactionRequest());
    final Pattern nonceWithValue = Pattern.compile("from\\\":\\\"(\\w*)\\\"");
    final Matcher matches = nonceWithValue.matcher(sendTransaction);
    return matches.replaceFirst(String.format("from\":\"%s\"", sender));
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

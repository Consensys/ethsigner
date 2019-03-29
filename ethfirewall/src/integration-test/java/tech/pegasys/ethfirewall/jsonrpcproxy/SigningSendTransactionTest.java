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

import static tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;
import static tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError.PARSE_ERROR;

import tech.pegasys.ethfirewall.jsonrpcproxy.model.EthFirewall;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.EthNode;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.Json;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

/** Signing is a step during proxying a sendTransaction() JSON-RPC request to an Ethereum node. */
public class SigningSendTransactionTest extends IntegrationTestBase {

  private final EthFirewall ethFirewall = new EthFirewall();
  private final EthNode ethNode = new EthNode();

  private static final String MALFORMED_JSON = "{Bad Json: {{{}";
  private static final Object NO_ID = null;

  @Test
  public void parseErrorResponseWhenJsonRequestIsMalformed() {
    sendVerifyingResponse(
        ethFirewall.request(MALFORMED_JSON), ethFirewall.response(NO_ID, PARSE_ERROR));
  }

  @Test
  public void proxyMalformedJsonResponseFromNode() {
    final String sendRawTransaction = defaultSendRawTransactionRequest();
    setUpEthNodeResponse(ethNode.request(sendRawTransaction), ethNode.response(MALFORMED_JSON));

    sendVerifyingResponse(
        ethFirewall.request(sendRawTransaction), ethFirewall.response(MALFORMED_JSON));
  }

  @Test
  public void invalidParamsResponseWhenNonceIsNaN() {
    sendVerifyingResponse(
        ethFirewall.request(defaultSendTransactionRequestWithNonce("I'm an invalid nonce format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void internalErrorResponseWhenMissingNonce() {
    // TODO This behaviour will change with the get nonce (PIE-1468)
    sendVerifyingResponse(
        ethFirewall.request(defaultSendTransactionRequestNoNonce()),
        ethFirewall.response(INTERNAL_ERROR));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooShort() {
    sendVerifyingResponse(
        ethFirewall.request(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f0723315")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooLong() {
    sendVerifyingResponse(
        ethFirewall.request(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f07233155A")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsMalformedHex() {
    sendVerifyingResponse(
        ethFirewall.request(
            defaultSendTransactionRequestWithSender("0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenSenderAddressIsEmpty() {
    final String sendTransactionRequest = defaultSendTransactionRequestWithSender("");
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c0940000000000000000000000000000000000000000849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a09d99057d1cb7a52c62c6e81ebf0e14516c5e93812f9a91beaa4576b05242ced4a04a87eefa7aa1240da54d0809f2867526cb726d93c064154a9855c30be6b190e8");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527333");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenMissingSenderAddress() {
    sendVerifyingResponse(
        ethFirewall.request(defaultSendTransactionRequestNoSender()),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenReceiverAddressIsTooShort() {
    sendVerifyingResponse(
        ethFirewall.request(
            defaultSendTransactionRequestWithReceiver("0xb60e8dd61c5d32be8058bb8eb970870f0723315")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenReceiverAddressIsTooLong() {
    sendVerifyingResponse(
        ethFirewall.request(
            defaultSendTransactionRequestWithReceiver(
                "0xb60e8dd61c5d32be8058bb8eb970870f07233155A")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsWhenReceiverAddressIsMalformedHex() {
    sendVerifyingResponse(
        ethFirewall.request(
            defaultSendTransactionRequestWithReceiver(
                "0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenReceiverAddressIsEmpty() {
    final String sendTransactionRequest = defaultSendTransactionRequestWithReceiver("");
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1524444");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signTransactionWhenMissingReceiverAddress() {
    final String sendTransactionRequest = defaultSendTransactionRequestNoReceiver();
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf89ea0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c080849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a09667b2df27d9bed3df507cbe4a0df47038934c350e442349546bedff0ebbe005a077d738b7c379683114694e98ddff0930a03ba1693fbb8ae597afc689757d9c6d");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1355555");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signTransactionWhenMissingValue() {
    final String sendTransactionRequest = defaultSendTransactionRequestNoValue();
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8aea0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a09909c15a400a8ee08025883c3f136287f2129b48cd5f3ebfae5f344c0deeb0e9a0382f174debaaee54054c9a2cb0ddc71cde22da321e83c940f34b24c1291236b9");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1666666");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenValueIsNaN() {
    sendVerifyingResponse(
        ethFirewall.request(defaultSendTransactionRequestWithValue("I'm an invalid value format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGas() {
    final String sendTransactionRequest = defaultSendTransactionRequestNoGas();
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8aca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2808276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0e22863d8ad64de06fe67f5d22eb19bf90ef26cb089a4912f846b42b10fa6027aa019b9851d46f1f5098b30a68133755d14e9bb43b86de750d9363b0cb7e1d6f939");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d7777777");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasIsNaN() {
    sendVerifyingResponse(
        ethFirewall.request(defaultSendTransactionRequestWithGas("I'm an invalid gas format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGasPrice() {
    final String sendTransactionRequest = defaultSendTransactionRequestNoGasPrice();
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b3a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a00083015f9094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0f6950faa587a4e12ac799ed27b247384684c4ea35274708fc25095a82df4abe0a02d13f3b28eff9a51eeb3a8e97975c43f41d79ec704bbfd9519be4bc27a255fcc");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasPriceIsNaN() {
    sendVerifyingResponse(
        ethFirewall.request(
            defaultSendTransactionRequestWithGasPrice("I'm an invalid gas price format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenMissingData() {
    sendVerifyingResponse(
        ethFirewall.request(defaultSendTransactionRequestNoData()),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signSendTransactionWhenContract() {
    final String sendTransactionRequest = smartContractSendTransactionRequest();
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf90182018083015f908080b90134608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a35a0d23d332cad1010a308bf531ccbb8985349eedfe9ec823b34c6e7fbddcba02420a048c9496343c72828e15dac889dfbb2aa5d9b2c23e87357e571350375aef90651");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthereumNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signSendTransaction() {
    final String sendTransactionRequest = defaultSendTransactionRequest();
    final String sendRawTransactionRequest =
        sendRawTransactionRequest(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    final String sendRawTransactionResponse =
        sendRawTransactionResponse(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102999999999");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendVerifyingResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

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
  private String defaultSendTransactionRequest() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String smartContractSendTransactionRequest() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestNoSender() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestNoNonce() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestNoReceiver() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestNoValue() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestNoGas() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestNoGasPrice() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestNoData() {
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
    return Json.encode(sendTransactionRequest);
  }

  private String defaultSendTransactionRequestWithGas(final String gas) {
    return replaceParameter("gas", gas, defaultSendTransactionRequest());
  }

  private String defaultSendTransactionRequestWithGasPrice(final String gasPrice) {
    return replaceParameter("gasPrice", gasPrice, defaultSendTransactionRequest());
  }

  private String defaultSendTransactionRequestWithValue(final String value) {
    return replaceParameter("value", value, defaultSendTransactionRequest());
  }

  private String defaultSendTransactionRequestWithNonce(final String nonce) {
    return replaceParameter("nonce", nonce, defaultSendTransactionRequest());
  }

  private String defaultSendTransactionRequestWithReceiver(final String receiver) {
    return replaceParameter("from", receiver, defaultSendTransactionRequest());
  }

  private String defaultSendTransactionRequestWithSender(final String sender) {
    return replaceParameter("to", sender, defaultSendTransactionRequest());
  }

  private String replaceParameter(
      final String key, final String replacementValue, final String body) {
    final Pattern nonceWithValue = Pattern.compile(String.format("%s\\\":\\\"(\\w*)\\\"", key));
    final Matcher matches = nonceWithValue.matcher(body);
    return matches.replaceFirst(String.format("%s\":\"%s\"", key, replacementValue));
  }

  private String defaultSendRawTransactionRequest() {
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        jsonRpc()
            .ethSendRawTransaction(
                "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    sendRawTransactionRequest.setId(77);

    return Json.encode(sendRawTransactionRequest);
  }

  private String sendRawTransactionRequest(final String value) {
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        jsonRpc().ethSendRawTransaction(value);
    sendRawTransactionRequest.setId(77);

    return Json.encode(sendRawTransactionRequest);
  }

  private String sendRawTransactionResponse(final String value) {
    final Response<String> sendRawTransactionResponse = new EthSendTransaction();
    sendRawTransactionResponse.setResult(value);
    return Json.encode(sendRawTransactionResponse);
  }
}

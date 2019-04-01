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

import tech.pegasys.ethfirewall.jsonrpcproxy.model.jsonrpc.SendRawTransaction;
import tech.pegasys.ethfirewall.jsonrpcproxy.model.jsonrpc.SendTransaction;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.CipherException;

/** Signing is a step during proxying a sendTransaction() JSON-RPC request to an Ethereum node. */
public class SigningSendTransactionTest extends IntegrationTestBase {

  private static final String MALFORMED_JSON = "{Bad Json: {{{}";
  private static final Object NO_ID = null;

  private SendTransaction sendTransaction;
  private SendRawTransaction sendRawTransaction;

  @Before
  public void setUp() {
    sendTransaction = new SendTransaction(jsonRpc());
    sendRawTransaction = new SendRawTransaction(jsonRpc());
  }

  @Test
  public void parseErrorResponseWhenJsonRequestIsMalformed() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(MALFORMED_JSON), ethFirewall.response(NO_ID, PARSE_ERROR));
  }

  @Test
  public void proxyMalformedJsonResponseFromNode() {
    final String rawTransaction = sendRawTransaction.request();
    setUpEthNodeResponse(ethNode.request(rawTransaction), ethNode.response(MALFORMED_JSON));

    sendRequestThenVerifyResponse(
        ethFirewall.request(rawTransaction), ethFirewall.response(MALFORMED_JSON));
  }

  @Test
  public void invalidParamsResponseWhenNonceIsNaN() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransaction.withNonce("I'm an invalid nonce format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void internalErrorResponseWhenMissingNonce() {
    // TODO This behaviour will change with the get nonce (PIE-1468)
    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransaction.missingNonce()), ethFirewall.response(INTERNAL_ERROR));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooShort() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(
            sendTransaction.withSender("0xb60e8dd61c5d32be8058bb8eb970870f0723315")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooLong() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(
            sendTransaction.withSender("0xb60e8dd61c5d32be8058bb8eb970870f07233155A")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsMalformedHex() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(
            sendTransaction.withSender("0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenSenderAddressIsEmpty() {
    final String sendTransactionRequest = sendTransaction.withSender("");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c0940000000000000000000000000000000000000000849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a09d99057d1cb7a52c62c6e81ebf0e14516c5e93812f9a91beaa4576b05242ced4a04a87eefa7aa1240da54d0809f2867526cb726d93c064154a9855c30be6b190e8");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527333");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenMissingSenderAddress() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransaction.missingSender()), ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenReceiverAddressIsTooShort() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(
            sendTransaction.withReceiver("0xb60e8dd61c5d32be8058bb8eb970870f0723315")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenReceiverAddressIsTooLong() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(
            sendTransaction.withReceiver("0xb60e8dd61c5d32be8058bb8eb970870f07233155A")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsWhenReceiverAddressIsMalformedHex() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(
            sendTransaction.withReceiver("0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenReceiverAddressIsEmpty() {
    final String sendTransactionRequest = sendTransaction.withReceiver("");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1524444");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signTransactionWhenMissingReceiverAddress() {
    final String sendTransactionRequest = sendTransaction.missingReceiver();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf89ea0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c080849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a09667b2df27d9bed3df507cbe4a0df47038934c350e442349546bedff0ebbe005a077d738b7c379683114694e98ddff0930a03ba1693fbb8ae597afc689757d9c6d");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1355555");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signTransactionWhenMissingValue() {
    final String sendTransactionRequest = sendTransaction.missingValue();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8aea0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a09909c15a400a8ee08025883c3f136287f2129b48cd5f3ebfae5f344c0deeb0e9a0382f174debaaee54054c9a2cb0ddc71cde22da321e83c940f34b24c1291236b9");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1666666");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenValueIsNaN() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransaction.withValue("I'm an invalid value format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGas() {
    final String sendTransactionRequest = sendTransaction.missingGas();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8aca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2808276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0e22863d8ad64de06fe67f5d22eb19bf90ef26cb089a4912f846b42b10fa6027aa019b9851d46f1f5098b30a68133755d14e9bb43b86de750d9363b0cb7e1d6f939");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d7777777");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasIsNaN() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransaction.withGas("I'm an invalid gas format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGasPrice() {
    final String sendTransactionRequest = sendTransaction.missingGasPrice();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8b3a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a00083015f9094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0f6950faa587a4e12ac799ed27b247384684c4ea35274708fc25095a82df4abe0a02d13f3b28eff9a51eeb3a8e97975c43f41d79ec704bbfd9519be4bc27a255fcc");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasPriceIsNaN() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransaction.withGasPrice("I'm an invalid gas price format!")),
        ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenMissingData() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransaction.missingData()), ethFirewall.response(INVALID_PARAMS));
  }

  @Test
  public void signSendTransactionWhenContract() {
    final String sendTransactionRequest = sendTransaction.smartContract();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf90182018083015f908080b90134608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a35a0d23d332cad1010a308bf531ccbb8985349eedfe9ec823b34c6e7fbddcba02420a048c9496343c72828e15dac889dfbb2aa5d9b2c23e87357e571350375aef90651");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signSendTransactionWhenContractWithLongChainId() throws IOException, CipherException {
    setupEthFirewall(4123123123L);

    final String sendTransactionRequest = sendTransaction.smartContract();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf90187018083015f908080b90134608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a8501eb83bb8aa0acd39a6db365d79db50c134392b64d735124a2c1d9abdc08cd55328689f86b8ba019a667f24696e37b14bab1117112c1f2b57b2e206826467abc66992a2524002b");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);

    resetEthFirewall();
  }

  @Test
  public void signSendTransaction() {
    final String sendTransactionRequest = sendTransaction.request();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8b2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567849184e72aa9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0f04e0e7b41adea417596550611138a3ec9a452abb6648d734107c53476e76a27a05b826d9e9b4e0dd0e7b8939c102a2079d71cfc27cd6b7bebe5a006d5ad17d780");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102999999999");
    setUpEthNodeResponse(
        ethNode.request(sendRawTransactionRequest), ethNode.response(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        ethFirewall.request(sendTransactionRequest),
        ethFirewall.response(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  // TODO change the chainID when signing
}

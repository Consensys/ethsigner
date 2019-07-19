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

import static java.math.BigInteger.ONE;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.NONCE_TOO_LOW;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.ethsigner.jsonrpcproxy.support.TransactionCountResponder.TRANSACTION_COUNT_METHOD.EEA_GET_TRANSACTION_COUNT;

import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendRawTransaction;
import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendTransaction;
import tech.pegasys.ethsigner.jsonrpcproxy.support.TransactionCountResponder;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.CipherException;

/** Signing is a step during proxying a sendTransaction() JSON-RPC request to an Ethereum node. */
public class SigningEeaSendTransactionIntegrationTest extends IntegrationTestBase {

  private EeaSendTransaction sendTransaction;
  private EeaSendRawTransaction sendRawTransaction;

  @Before
  public void setUp() {
    sendTransaction = new EeaSendTransaction();
    sendRawTransaction = new EeaSendRawTransaction(eeaJsonRpc());
    final TransactionCountResponder getTransactionResponse =
        new TransactionCountResponder(nonce -> nonce.add(ONE), EEA_GET_TRANSACTION_COUNT);
    clientAndServer.when(getTransactionResponse.request()).respond(getTransactionResponse);
  }

  @Test
  public void proxyMalformedJsonResponseFromNode() {
    final String rawTransaction = sendRawTransaction.request();
    setUpEthNodeResponse(request.ethNode(rawTransaction), response.ethNode(MALFORMED_JSON));

    sendRequestThenVerifyResponse(
        request.ethSigner(rawTransaction), response.ethSigner(MALFORMED_JSON));
  }

  @Test
  public void invalidParamsResponseWhenNonceIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withNonce("I'm an invalid nonce format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void missingNonceResultsInEthNodeRespondingSuccessfully() {
    final String ethNodeResponseBody = "VALID_RESPONSE";
    final String requestBody =
        sendRawTransaction.request(
            "0xf8fb018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a007e8df0227e6321947df9df4afc8cdc09f9ee4f7171aa96509081779b75a2674a0672b990f69418c987ee95d13c1e9f9f5e3310866c09dc82fe56cec5edcce3025b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    setUpEthNodeResponse(request.ethNode(requestBody), response.ethNode(ethNodeResponseBody));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(ethNodeResponseBody));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooShort() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("0x577919ae5df4941180eac211965f275CDCE314D")),
        response.ethSigner(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsTooLong() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("0x1577919ae5df4941180eac211965f275CDCE314D")),
        response.ethSigner(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressMissingHexPrefix() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("7577919ae5df4941180eac211965f275CDCE314D")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenSenderAddressIsMalformedHex() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        response.ethSigner(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
  }

  @Test
  public void invalidParamsWhenSenderAddressIsEmpty() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("")), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenSenderAddressCaseMismatchesUnlockedAccount() {
    final String sendTransactionRequest =
        sendTransaction.withSender("0x7577919ae5df4941180eac211965f275CDCE314D");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf9011ba0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a01472185b9d9fc1c2e268592f6e209c7c73831985808a741fe2f3dcc06d750928a0449a0e7f7d8b126b569b64b3ac5c9dbd590566335d269b2c281d17590e12f302b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1666666");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenMissingSenderAddress() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingSender()), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenReceiverAddressIsEmpty() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withReceiver("")), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenReceiverAddressMissingHexPrefix() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withReceiver("7577919ae5df4941180eac211965f275CDCE314D")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingReceiverAddress() {
    final String sendTransactionRequest = sendTransaction.missingReceiver();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf90107a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0ddec0fd0654c3b3e97aa75f8e7ec426e97df7e61fa9cf8d02bc2dfe27838fbada04530fb0f1193b12676c7de7bae79dd2f1df0bd939e1d419aa6c0aff0c1925aa9b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1355555");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signTransactionWhenMissingValue() {
    final String sendTransactionRequest = sendTransaction.missingValue();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf90107a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0ddec0fd0654c3b3e97aa75f8e7ec426e97df7e61fa9cf8d02bc2dfe27838fbada04530fb0f1193b12676c7de7bae79dd2f1df0bd939e1d419aa6c0aff0c1925aa9b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1666666");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenValueIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withValue("I'm an invalid value format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGas() {
    final String sendTransactionRequest = sendTransaction.missingGas();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf90108a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a00083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0edf7a5f6e94518a974bbaeac692186b549af842b1c481809762b9605a3c5d84ca0790ccfb63e4c4f18fc7503d605b1dea49c902199f184ffdef97efe63bcd5b29bb36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d7777777");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withGas("I'm an invalid gas format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void signTransactionWhenMissingGasPrice() {
    final String sendTransactionRequest = sendTransaction.missingGasPrice();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf90115a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2808276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0dd41a5dd966d2190ff6b9b2f6fd3b1b7dc2210a8fbc2cedb343ab12da07dee57a05bd64eec09cf2e788e7aa6e8288538ee9c5365498faac492fa0a51748360fe78b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void invalidParamsResponseWhenGasPriceIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withGasPrice("I'm an invalid gas price format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void signSendTransactionWhenMissingData() {
    final String sendTransactionRequest = sendTransaction.missingData();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8f2a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567808035a06e11c7a9a383328ec28fa55a00ffb2641f3a9e3eee2f29d5863a645b025e6021a04ab2958d818b1384b5c5a665e816579ae4d1b29f592c55054c46d52996b87ae5b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102999999999");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signSendTransactionWhenContract() {
    final String sendTransactionRequest = sendTransaction.smartContract();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8e2018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a089192eedb5d11a974f51bd9f9a9c5f3df229332a8f4058fe093719e68c4da2f5a055205f789696320ec0a2dc679d431a39bda3393d57d43f467e126e7cc07add36b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void signSendTransactionWhenContractWithLongChainId() throws IOException, CipherException {
    setupEthSigner(4123123123L);

    final String sendTransactionRequest = sendTransaction.smartContract();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8e7018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f0724456758501eb83bb8aa0dea5b5d37de403ebf73a3e9c0f8489edf0daae70ea1a5e02989739249d9c8626a03f6324caa196a12aff6e4de8313c65bc6b848377b22c78357ae66d335e71ae71b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102688888888");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);

    resetEthSigner();
  }

  @Test
  public void signSendTransaction() {
    final String sendTransactionRequest = sendTransaction.request();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf9011ba0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a01472185b9d9fc1c2e268592f6e209c7c73831985808a741fe2f3dcc06d750928a0449a0e7f7d8b126b569b64b3ac5c9dbd590566335d269b2c281d17590e12f302b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");
    final String sendRawTransactionResponse =
        sendRawTransaction.response(
            "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d0592102999999999");
    setUpEthNodeResponse(
        request.ethNode(sendRawTransactionRequest), response.ethNode(sendRawTransactionResponse));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransactionRequest), response.ethSigner(sendRawTransactionResponse));

    verifyEthNodeReceived(sendRawTransactionRequest);
  }

  @Test
  public void missingNonceResultsInNewNonceBeingCreatedAndResent() {
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(
            "0xf8fb028609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0f41672ced60bed6ae22438226167fc5a98c85c51117ddc8f6db53b7eedd42816a025a258e35a0929aee655905fed6f0be31d2a4e9fc200759227915f3e9034e9ccb36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");

    final String rawTransactionWithNextNonce =
        sendRawTransaction.request(
            "0xf8fb018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a007e8df0227e6321947df9df4afc8cdc09f9ee4f7171aa96509081779b75a2674a0672b990f69418c987ee95d13c1e9f9f5e3310866c09dc82fe56cec5edcce3025b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");

    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithInitialNonce), response.ethNode(NONCE_TOO_LOW));

    final String successResponseFromWeb3Provider = "VALID_RESULT";
    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithNextNonce),
        response.ethNode(successResponseFromWeb3Provider));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()),
        response.ethSigner(successResponseFromWeb3Provider));
  }

  @Test
  public void transactionWithMissingNonceReturnsErrorsOtherThanLowNonceToCaller() {
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(
            "0xf8fb018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a007e8df0227e6321947df9df4afc8cdc09f9ee4f7171aa96509081779b75a2674a0672b990f69418c987ee95d13c1e9f9f5e3310866c09dc82fe56cec5edcce3025b36656c2a912c3897dc2a832c38fc38bc3b7c2bcc3b3c3afc3bac38ac3b0c29411522f1fc38dc3b2c390c39e00c3ab01c3ae2972edac195f26c391564071c3860600060c06c3a610c2b4c2a123c3917b695dc3a6c2b0c39803c39cc2a019c2ad036c8a72657374726963746564");

    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithInitialNonce), response.ethNode(INVALID_PARAMS));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenMissingPrivateFrom() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingPrivateFrom()),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenMissingPrivateFor() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingPrivateFor()), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  public void invalidParamsResponseWhenMissingRestriction() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingRestriction()),
        response.ethSigner(INVALID_PARAMS));
  }
}

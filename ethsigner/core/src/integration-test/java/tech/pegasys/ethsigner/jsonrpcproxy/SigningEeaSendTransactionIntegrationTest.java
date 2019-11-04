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
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.ethsigner.jsonrpcproxy.support.TransactionCountResponder.TRANSACTION_COUNT_METHOD.EEA_GET_TRANSACTION_COUNT;

import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendRawTransaction;
import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendTransaction;
import tech.pegasys.ethsigner.jsonrpcproxy.support.TransactionCountResponder;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Signing is a step during proxying a sendTransaction() JSON-RPC request to an Ethereum node. */
class SigningEeaSendTransactionIntegrationTest extends IntegrationTestBase {

  private EeaSendTransaction sendTransaction;
  private EeaSendRawTransaction sendRawTransaction;

  @BeforeEach
  void setUp() {
    sendTransaction = new EeaSendTransaction();
    sendRawTransaction = new EeaSendRawTransaction(eeaJsonRpc());
    final TransactionCountResponder getTransactionResponse =
        new TransactionCountResponder(nonce -> nonce.add(ONE), EEA_GET_TRANSACTION_COUNT);
    clientAndServer.when(getTransactionResponse.request()).respond(getTransactionResponse);
  }

  @Test
  void proxyMalformedJsonResponseFromNode() {
    final String rawTransaction = sendRawTransaction.request();
    setUpEthNodeResponse(request.ethNode(rawTransaction), response.ethNode(MALFORMED_JSON));

    sendRequestThenVerifyResponse(
        request.ethSigner(rawTransaction), response.ethSigner(MALFORMED_JSON));
  }

  @Test
  void invalidParamsResponseWhenNonceIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withNonce("I'm an invalid nonce format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void invalidParamsResponseWhenSenderAddressIsTooShort() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("0x577919ae5df4941180eac211965f275CDCE314D")),
        response.ethSigner(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
  }

  @Test
  void invalidParamsResponseWhenSenderAddressIsTooLong() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("0x1577919ae5df4941180eac211965f275CDCE314D")),
        response.ethSigner(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
  }

  @Test
  void invalidParamsResponseWhenSenderAddressMissingHexPrefix() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("7577919ae5df4941180eac211965f275CDCE314D")),
        response.ethSigner(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
  }

  @Test
  void invalidParamsResponseWhenSenderAddressIsMalformedHex() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("0xb60e8dd61c5d32be8058bb8eb970870f07233XXX")),
        response.ethSigner(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
  }

  @Test
  void invalidParamsWhenSenderAddressIsEmpty() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withSender("")), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void signTransactionWhenSenderAddressCaseMismatchesUnlockedAccount() {
    final String sendTransactionRequest =
        sendTransaction.withSender("0x7577919ae5df4941180eac211965f275CDCE314D");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8fca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a045294f33c6d9fb186a1f001d56c026ede8ba18c48b4c94e929f9fdeab962a632a060a1ff5c812a984b613e4c7caad626ee61a5711c1498311e8e001f05ba7653eda06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void invalidParamsResponseWhenMissingSenderAddress() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingSender()), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void signTransactionWhenReceiverAddressIsEmpty() {
    final String sendTransactionRequest = sendTransaction.withReceiver("");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8e8a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a04fad7672bda5c3b7fa5c3cae6443f810ee880e9682ca5d67e1718ed72385c7fea00249e9d794a42c6d772dca2adee6c5b6daf37989d353460330ad90817f42bddea06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void signTransactionWhenEmptyReceiverAddress() {
    final String sendTransactionRequest = sendTransaction.withReceiver("");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8e8a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a04fad7672bda5c3b7fa5c3cae6443f810ee880e9682ca5d67e1718ed72385c7fea00249e9d794a42c6d772dca2adee6c5b6daf37989d353460330ad90817f42bddea06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void signTransactionWhenReceiverHasAddressMissingHexPrefix() {
    final String sendTransactionRequest =
        sendTransaction.withReceiver("7577919ae5df4941180eac211965f275CDCE314D");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8fca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c0947577919ae5df4941180eac211965f275cdce314d80a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0ef2ec6ed6efff22f4682be538d18d88953673459b9607013797aa7c7e07af381a00e3dc55d0619660fbcc76bcec5c4e481d4c34cad514a81ab179612db80217b86a06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void signTransactionWhenMissingReceiverAddress() {
    final String sendTransactionRequest = sendTransaction.missingReceiver();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8e8a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a04fad7672bda5c3b7fa5c3cae6443f810ee880e9682ca5d67e1718ed72385c7fea00249e9d794a42c6d772dca2adee6c5b6daf37989d353460330ad90817f42bddea06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void signTransactionWhenMissingValue() {
    final String sendTransactionRequest = sendTransaction.missingValue();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8e8a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a04fad7672bda5c3b7fa5c3cae6443f810ee880e9682ca5d67e1718ed72385c7fea00249e9d794a42c6d772dca2adee6c5b6daf37989d353460330ad90817f42bddea06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void invalidParamsResponseWhenValueIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withValue("I'm an invalid value format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void signTransactionWhenMissingGas() {
    final String sendTransactionRequest = sendTransaction.missingGas();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8e9a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a00083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0aa1b8a27d5504b4431dde8fe9ed931efbf648e062676b72d8e26cf4120004a84a06574a8edacbadee7b71381e9b0463fd20f1c224ba77aa663a0b2e52b56819bc4a06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void invalidParamsResponseWhenGasIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withGas("I'm an invalid gas format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void signTransactionWhenMissingGasPrice() {
    final String sendTransactionRequest = sendTransaction.missingGasPrice();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8f6a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2808276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a085e1d13afa0fbe15042c1726fc1e0d5f7fac0791e51365c266f5ac7d59ec32faa074d159e9d94028ac7dbfc01ce20b9462990b4b4df8e04bef70341cc795d64050a06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void invalidParamsResponseWhenGasPriceIsNaN() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withGasPrice("I'm an invalid gas price format!")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void signSendTransactionWhenMissingData() {
    final String sendTransactionRequest = sendTransaction.missingData();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8d3a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567808036a0dc604f81129b681461fd7b3b20e0f9def8119dbc19a36e2a772a0d8622efdcd8a02e7fef7240e32b702bebf506a4a68ecc10c3442f1f0e2bb0e5636a652af23f21a06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void signSendTransactionWhenContract() {
    final String sendTransactionRequest = sendTransaction.smartContract();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8c3018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0906a6e968768df31f85eaaaa5f825141cb4805f449ffe9ca063e983a787bbc69a00692f403e9b86efcf96074e1e6376f30728528fb9c2d24b5359dec89dbef6c6da06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void signSendTransactionWhenContractWithLongChainId() throws IOException {
    setupEthSigner(4123123123L);

    final String sendTransactionRequest = sendTransaction.smartContract();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8c8018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f0724456758501eb83bb89a0c8aee2c28c2680fd99cc7251d2230f788b57bac8d551bd7acf30dc938bc98ecda021f9d91dc50c28a2e5cde83020b71b5907886ef456ef6b1eeb542aac40e59d5da06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void signSendTransaction() {
    final String sendTransactionRequest = sendTransaction.request();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf8fca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a045294f33c6d9fb186a1f001d56c026ede8ba18c48b4c94e929f9fdeab962a632a060a1ff5c812a984b613e4c7caad626ee61a5711c1498311e8e001f05ba7653eda06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");
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
  void missingNonceInPrivateTransactionIsPopulated() {
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(
            "0xf8dc018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a039231358760e94952178f2963c32a977cbca58e9ebfbbd6263f8ca32b280516ea01d12666bea8dc7d4fb747c6dea691a7b90098c921e7f4adac8afd6178cbeb5a7a06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");

    final String successResponseFromWeb3Provider = "VALID_RESULT";
    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithInitialNonce),
        response.ethNode(successResponseFromWeb3Provider));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()),
        response.ethSigner(successResponseFromWeb3Provider));
  }

  @Test
  void transactionWithMissingNonceReturnsErrorsOtherThanLowNonceToCaller() {
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(
            "0xf8dc018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a039231358760e94952178f2963c32a977cbca58e9ebfbbd6263f8ca32b280516ea01d12666bea8dc7d4fb747c6dea691a7b90098c921e7f4adac8afd6178cbeb5a7a06656a912c97da832cfcbf7bcf3effacaf09411522f1fcdf2d0de00eb01ee2972e1a0195f26d1564071c60600060c06e610b4a123d17b695de6b0d803dca019ad036c8a72657374726963746564");

    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithInitialNonce), response.ethNode(INVALID_PARAMS));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void invalidParamsResponseWhenMissingPrivateFrom() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingPrivateFrom()),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void invalidParamsResponseWhenMissingPrivateFor() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingPrivateFor()), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void invalidParamsResponseWhenMissingRestriction() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingRestriction()),
        response.ethSigner(INVALID_PARAMS));
  }
}

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

import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static java.math.BigInteger.ONE;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.NONCE_TOO_LOW;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_DATA_DEFAULT;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS_DEFAULT;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS_PRICE_DEFAULT;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_VALUE_DEFAULT;
import static tech.pegasys.ethsigner.jsonrpcproxy.support.TransactionCountResponder.TRANSACTION_COUNT_METHOD.EEA_GET_TRANSACTION_COUNT;

import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendRawTransaction;
import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendTransaction;
import tech.pegasys.ethsigner.jsonrpcproxy.support.TransactionCountResponder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

/** Signing is a step during proxying a sendTransaction() JSON-RPC request to an Ethereum node. */
class SigningEeaSendTransactionIntegrationTest extends IntegrationTestBase {

  private EeaSendTransaction sendTransaction;
  private EeaSendRawTransaction sendRawTransaction;

  @BeforeEach
  void setUp() {
    sendTransaction = new EeaSendTransaction();
    sendRawTransaction = new EeaSendRawTransaction(eeaJsonRpc(), credentials);
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
  void missingNonceResultsInEthNodeRespondingSuccessfully() {
    final String ethNodeResponseBody = "VALID_RESPONSE";
    final String requestBody = sendRawTransaction.request(sendTransaction.withNonce("0x1"));

    setUpEthNodeResponse(request.ethNode(requestBody), response.ethNode(ethNodeResponseBody));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(ethNodeResponseBody));
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
    final Request<?, EthSendTransaction> sendTransactionRequest =
        sendTransaction.withSender("0x7577919ae5df4941180eac211965f275CDCE314D");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            sendTransaction.withSender("0x7577919ae5df4941180eac211965f275cdce314d"));
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
    final Request<Object, EthSendTransaction> sendTransactionRequest =
        sendTransaction.withReceiver("");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.missingReceiver());
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
    final Request<Object, EthSendTransaction> sendTransactionRequest =
        sendTransaction.withReceiver("");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.missingReceiver());
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
    final Request<Object, EthSendTransaction> sendTransactionRequest =
        sendTransaction.withReceiver("7577919ae5df4941180eac211965f275CDCE314D");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            sendTransaction.withReceiver("0x7577919ae5df4941180eac211965f275CDCE314D"));
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
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.missingReceiver();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.missingReceiver());
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
  void signTransactionWhenReceiverAddressIsNull() {
    final Request<?, EthSendTransaction> sendTransactionRequest =
        sendTransaction.withReceiver(null);
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.missingReceiver());
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
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.missingValue();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.withValue(FIELD_VALUE_DEFAULT));
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
  void signTransactionWhenValueIsNull() {
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.withValue(null);
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.withValue(FIELD_VALUE_DEFAULT));
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
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.missingGas();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.withGas(FIELD_GAS_DEFAULT));

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
  void signTransactionWhenGasIsNull() {
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.withGas(null);
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.withGas(FIELD_GAS_DEFAULT));

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
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.missingGasPrice();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.withGasPrice(FIELD_GAS_PRICE_DEFAULT));
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
  void signTransactionWhenGasPriceIsNull() {
    final Request<?, EthSendTransaction> sendTransactionRequest =
        sendTransaction.withGasPrice(null);
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.withGasPrice(FIELD_GAS_PRICE_DEFAULT));
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
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.missingData();
    final String sendRawTransactionRequest =
        sendRawTransaction.request(sendTransaction.withData(FIELD_DATA_DEFAULT));
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
  void signSendTransaction() {
    final Request<?, EthSendTransaction> sendTransactionRequest = sendTransaction.request();
    final String sendRawTransactionRequest = sendRawTransaction.request(sendTransaction.request());
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
  void missingNonceResultsInNewNonceBeingCreatedAndResent() {
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(sendTransaction.withNonce("0x0"));
    final String rawTransactionWithNextNonce =
        sendRawTransaction.request(sendTransaction.withNonce("0x1"));
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
  void nullNonceResultsInNewNonceBeingCreatedAndResent() {
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(sendTransaction.withNonce("0x0"));
    final String rawTransactionWithNextNonce =
        sendRawTransaction.request(sendTransaction.withNonce("0x1"));
    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithInitialNonce), response.ethNode(NONCE_TOO_LOW));

    final String successResponseFromWeb3Provider = "VALID_RESULT";
    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithNextNonce),
        response.ethNode(successResponseFromWeb3Provider));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withNonce(null)),
        response.ethSigner(successResponseFromWeb3Provider));
  }

  @Test
  void missingNonceInPrivateTransactionIsPopulated() {
    final String rawTransactionWithInitialNonce =
        sendRawTransaction.request(sendTransaction.withNonce("0x1"));

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
        sendRawTransaction.request(sendTransaction.withNonce("0x1"));
    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithInitialNonce), response.ethNode(INVALID_PARAMS));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void moreThanFiveNonceTooLowErrorsReturnsAnErrorToUser() {
    setupEthNodeResponse(".*eea_sendRawTransaction.*", response.ethNode(NONCE_TOO_LOW), 6);

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(INTERNAL_ERROR));
  }

  @Test
  void thirdNonceRetryTimesOutAndGatewayTimeoutIsReturnedToClient() {
    setupEthNodeResponse(".*eea_sendRawTransaction.*", response.ethNode(NONCE_TOO_LOW), 3);
    timeoutRequest(".*eea_sendRawTransaction.*");

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()),
        response.ethSigner(CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT, GATEWAY_TIMEOUT));
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
  void invalidParamsResponseWhenPrivateForIsNull() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withPrivateFor(null)),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void invalidParamsResponseWhenMissingRestriction() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingRestriction()),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void invalidParamsResponseWhenRestrictionIsNull() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withRestriction(null)),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void invalidParamsResponseWhenRestrictionHasInvalidValue() {
    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.withRestriction("invalid")),
        response.ethSigner(INVALID_PARAMS));
  }

  @Test
  void missingNonceResultsInRequestToPrivGetEeaTransactionCount() {
    final String ethNodeResponseBody = "VALID_RESPONSE";
    final String requestBody = sendRawTransaction.request(sendTransaction.withNonce("0x1"));

    setUpEthNodeResponse(request.ethNode(requestBody), response.ethNode(ethNodeResponseBody));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(ethNodeResponseBody));

    verifyEthNodeReceivedGetEeaTransactionCountRequestOnce();
  }
}

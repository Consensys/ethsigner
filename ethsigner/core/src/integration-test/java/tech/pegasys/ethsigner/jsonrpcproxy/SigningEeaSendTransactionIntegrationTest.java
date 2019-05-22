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

import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.NONCE_TOO_LOW;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendRawTransaction;
import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendTransaction;

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
            "0xf8f4018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a08318ba5cc03033728e76ee84539f8717f1fd5f0dda3744080d2517f608e95373a046e0448adcffcc1fa58ead8d7bceb48f6388e4237b9b510d51bdc64904a35ffaac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf90114a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a01003c458155c543d980f2bed35c6387b4a89e4230a13d8cba5928d7eb10421eba05d8c26ceff2e8cf7cf94d748a243af989688fa121f57dc9c586342e92a225ed1ac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf8fca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a09b76e47cbd31cf6e802436300513f20780327bca86119aed2a123bc9a3c3cdb1a001157bec8b7b29d86cbb17c93bb1e992cfa5074250badd31162c7e8917fc9d10aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf90100a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a09f2ee982793eb84627a715db3c23992120b35efc0de671d11db959e0650b6471a074e190118f58ed589f7449ec514d62c0596038615ffab69b2f882dde3bc2970fac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf90101a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a00083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0ca56dc817f48b86861dcac15d06ea762b6aee4924fdc900735e887efde298046a05dfbf1962f641c2df78d4e86ce06e1f131509016756e175a0f1fd8226f7b1bfdac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf9010aa0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2808276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a03a0690705554b7b975d3e3e074d1fbd20bed3ef4c592877d9ad452b33cac3059a00e9d351eb605c1c8bfa2374c418a1aadca8f5699ac3c25702bf155fad8080d2caa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf8eba0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567808036a039fa7994efb80ac86e24878f6e09eca6cebb9da2801ebcdaaf361f5afc61d14ca0748d1c095debaef4be7cd575b83cca71a4c66cec936e1249088956b3591e336fac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf8db018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0a062612bde14fe234b974d2d4745c4b9fc5f99a3f98574d6c5c7d528bb229381a0295a7597e2ab95039f79e4d094e498cc04bae05306ac868f3a3fceb9482aed54ac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf8e0018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f0724456758501eb83bb89a03058ee15a2c76d08c7cf15994a6f47700dc35b4b890452a353f84bdda72c7b7aa0709aff91f08d192801af8d5232d910990d19af5dc8563d16e83d1255c7eeb05dac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf90114a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a01003c458155c543d980f2bed35c6387b4a89e4230a13d8cba5928d7eb10421eba05d8c26ceff2e8cf7cf94d748a243af989688fa121f57dc9c586342e92a225ed1ac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
  public void signSendTransactionWithNonZeroValue() {
    final String sendTransactionRequest = sendTransaction.withValue("0x4");
    final String sendRawTransactionRequest =
        sendRawTransaction.request(
            "0xf90114a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456704a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0fe72a92aede764ce41d06b163d28700b58e5ee8bb1af91d9d54979ea3bdb3e7ea046ae10c94c322fa44ddceb86677c2cd6cc17dfbd766924f41d10a244c512996dac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");
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
            "0xf8f1018609184e72a00083015f9094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a084adbdfe62bc6cfd2a0b9e82a2c34c562af330c76904a3945b3f5f7a10e0ef7ba05a073fb3fd0ef02a76702faaa4e29f4acb832e9fbd2d622ccf7e376e5662e912aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");

    final String rawTransactionWithNextNonce =
        sendRawTransaction.request(
            "0xf8f4018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a08318ba5cc03033728e76ee84539f8717f1fd5f0dda3744080d2517f608e95373a046e0448adcffcc1fa58ead8d7bceb48f6388e4237b9b510d51bdc64904a35ffaac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");

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
            "0xf8f4018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a08318ba5cc03033728e76ee84539f8717f1fd5f0dda3744080d2517f608e95373a046e0448adcffcc1fa58ead8d7bceb48f6388e4237b9b510d51bdc64904a35ffaac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564");

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

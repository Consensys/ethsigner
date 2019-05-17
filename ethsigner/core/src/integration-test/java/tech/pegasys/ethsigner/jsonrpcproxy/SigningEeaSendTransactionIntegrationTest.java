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
    sendTransaction = new EeaSendTransaction(eeaJsonRpc());
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
            "0xf8f0018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0029c9521fe7190c8852a2b38a4e933aeb8c1e064030732b558ed788192c0de9ea04b8b90fecbd813671d4228feac09dcd33e92d9e1513fdc93dd3090be78ff20e4aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf90110a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a00b528cefb87342b2097318cd493d13b4c9bd55bf35bf1b3cf2ef96ee14cee563a06423107befab5530c42a2d7d2590b96c04ee361c868c138b054d7886966121a6aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf8fca0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c08080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a09b76e47cbd31cf6e802436300513f20780327bca86119aed2a123bc9a3c3cdb1a001157bec8b7b29d86cbb17c93bb1e992cfa5074250badd31162c7e8917fc9d10aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf8fda0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a00083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0171c0334f40d47ae544a6c05f2c3e753bb9687688be2f9a2a1973874a38a8462a04eb00c8e7149c07de729098b7fb75342d81cf13a69e623ca1e1636e2ff7ccc4aaa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf8e7a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f07244567808036a0c792124d74ab5a9153423a5ac9332f8803568ba0396813cc10e9345ac5515493a03c638d8b3b92075477e4ea958f44fbcb40dca31e9bfd42bc08f6725ca93ce801aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf8d7018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0e2afc40f89b514e21b948357f44b9540d8702f8ed57ed96b25a36301bb77b7ffa06c9e11001b32b52dfcfbd09aede38e2a42c76608319bc4e5b9c9dc8215c0df4caa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf8dc018083015f908080a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f0724456758501eb83bb89a0f9ef6da62e5f54a89010c558be4e8faa43ab8248c0ca6f11c9bc03a597c09192a0046d734e0c32cdb09c6cc3c75c72f4418d25b6968f3815d050fde85e36605969aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf90110a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a00b528cefb87342b2097318cd493d13b4c9bd55bf35bf1b3cf2ef96ee14cee563a06423107befab5530c42a2d7d2590b96c04ee361c868c138b054d7886966121a6aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
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
            "0xf8f0018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0029c9521fe7190c8852a2b38a4e933aeb8c1e064030732b558ed788192c0de9ea04b8b90fecbd813671d4228feac09dcd33e92d9e1513fdc93dd3090be78ff20e4aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");

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
            "0xf8f0018609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567535a0029c9521fe7190c8852a2b38a4e933aeb8c1e064030732b558ed788192c0de9ea04b8b90fecbd813671d4228feac09dcd33e92d9e1513fdc93dd3090be78ff20e4aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");

    setUpEthNodeResponse(
        request.ethNode(rawTransactionWithInitialNonce), response.ethNode(INVALID_PARAMS));

    sendRequestThenVerifyResponse(
        request.ethSigner(sendTransaction.missingNonce()), response.ethSigner(INVALID_PARAMS));
  }
}

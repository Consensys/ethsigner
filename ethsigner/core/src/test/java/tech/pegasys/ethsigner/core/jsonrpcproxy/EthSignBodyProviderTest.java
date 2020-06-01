/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.core.jsonrpcproxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthSignBodyProvider;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.TransactionSigner;
import tech.pegasys.signers.secp256k1.api.TransactionSignerProvider;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.NullSource;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public class EthSignBodyProviderTest {

  @ParameterizedTest
  @ArgumentsSource(InvalidParamsProvider.class)
  @NullSource
  public void ifParamIsInvalidErrorIsReturned(final Object params) {
    final TransactionSignerProvider mockSignerProvider = mock(TransactionSignerProvider.class);
    final EthSignBodyProvider bodyProvider = new EthSignBodyProvider(mockSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(params);
    final JsonRpcBody body = bodyProvider.getBody(request);

    assertThat(body.hasError()).isTrue();
    assertThat(body.error().getCode()).isEqualTo(JsonRpcError.INVALID_PARAMS.getCode());
  }

  @Test
  public void ifAddressIsNotUnlockedErrorIsReturned() {
    final TransactionSignerProvider mockSignerProvider = mock(TransactionSignerProvider.class);
    final EthSignBodyProvider bodyProvider = new EthSignBodyProvider(mockSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(List.of("address", "message"));
    final JsonRpcBody body = bodyProvider.getBody(request);

    assertThat(body.hasError()).isTrue();
    assertThat(body.error().getCode())
        .isEqualTo(JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT.getCode());
  }

  @Test
  public void signatureHasTheExpectedFormat() {
    final TransactionSigner mockTransactionSigner = mock(TransactionSigner.class);
    final BigInteger v = BigInteger.ONE;
    final BigInteger r = BigInteger.TWO;
    final BigInteger s = BigInteger.TEN;
    doReturn(new Signature(v, r, s)).when(mockTransactionSigner).sign(any(byte[].class));
    final TransactionSignerProvider mockSignerProvider = mock(TransactionSignerProvider.class);
    doReturn(Optional.of(mockTransactionSigner)).when(mockSignerProvider).getSigner(anyString());
    final EthSignBodyProvider bodyProvider = new EthSignBodyProvider(mockSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(List.of("address", "message"));

    final JsonRpcBody body = bodyProvider.getBody(request);
    final JsonObject jsonObj = new JsonObject(body.body());

    assertThat(body.hasError()).isFalse();
    assertThat(jsonObj.getString("jsonrpc")).isEqualTo("2.0");
    assertThat(jsonObj.getInteger("id")).isEqualTo(id);
    final String hexSignature = jsonObj.getString("result");
    assertThat(hexSignature).hasSize(132);

    final byte[] signature = Numeric.hexStringToByteArray(hexSignature);

    assertThat(new BigInteger(1, signature, 0, 32)).isEqualTo(r);
    assertThat(new BigInteger(1, signature, 32, 32)).isEqualTo(s);
    assertThat(new BigInteger(1, signature, 64, 1)).isEqualTo(v);
  }

  @Test
  public void returnsExpectedSignature() {
    final ECKeyPair keyPair =
        ECKeyPair.create(
            Numeric.hexStringToByteArray(
                "0x1618fc3e47aec7e70451256e033b9edb67f4c469258d8e2fbb105552f141ae41"));
    final TransactionSigner mockTransactionSigner = mock(TransactionSigner.class);
    doAnswer(
            answer -> {
              byte[] data = answer.getArgument(0, byte[].class);
              final Sign.SignatureData signature = Sign.signMessage(data, keyPair);
              return new Signature(
                  new BigInteger(signature.getV()),
                  new BigInteger(1, signature.getR()),
                  new BigInteger(1, signature.getS()));
            })
        .when(mockTransactionSigner)
        .sign(any(byte[].class));

    final TransactionSignerProvider mockSignerProvider = mock(TransactionSignerProvider.class);
    doReturn(Optional.of(mockTransactionSigner)).when(mockSignerProvider).getSigner(anyString());
    final EthSignBodyProvider bodyProvider = new EthSignBodyProvider(mockSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(
        List.of(
            "address",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Tubulum"
                + " fuisse, qua illum, cuius is condemnatus est rogatione, P. Eaedem res maneant alio modo."));

    final JsonRpcBody body = bodyProvider.getBody(request);
    assertThat(body.hasError()).isFalse();
    final JsonObject jsonObj = new JsonObject(body.body());
    final String hexSignature = jsonObj.getString("result");
    final byte[] signature = Numeric.hexStringToByteArray(hexSignature);

    final ECDSASignature expectedSignature =
        keyPair.sign(
            Numeric.hexStringToByteArray(
                "0xe63325d74baa84af003dfb6a974f41672be881b56aa2c12c093f8259321bd460"));
    assertThat(new BigInteger(1, signature, 0, 32)).isEqualTo(expectedSignature.r);
    assertThat(new BigInteger(1, signature, 32, 32)).isEqualTo(expectedSignature.s);
  }

  private static class InvalidParamsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      return Stream.of(
          Arguments.of(Collections.emptyList()),
          Arguments.of(Collections.singleton(2)),
          Arguments.of(List.of(1, 2, 3)),
          Arguments.of(new Object()));
    }
  }
}

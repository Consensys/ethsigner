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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.ethsigner.core.AddressIndexedSignerProvider;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthSignTransactionResultProvider;
import tech.pegasys.signers.secp256k1.EthPublicKeyUtils;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.Signer;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.NullSource;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

public class EthSignTransactionResultProviderTest {

  private static JsonDecoder jsonDecoder;
  private static long chainId;

  @BeforeAll
  static void beforeAll() {
    final ObjectMapper jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
    jsonDecoder = new JsonDecoder(jsonObjectMapper);
    chainId = 44844;
  }

  @ParameterizedTest
  @ArgumentsSource(InvalidParamsProvider.class)
  @NullSource
  public void ifParamIsInvalidExceptionIsThrownWithInvalidParams(final Object params) {
    final AddressIndexedSignerProvider mockSignerProvider =
        mock(AddressIndexedSignerProvider.class);
    final EthSignTransactionResultProvider resultProvider =
        new EthSignTransactionResultProvider(chainId, mockSignerProvider, jsonDecoder);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTransaction");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(params);

    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void ifAddressIsNotUnlockedExceptionIsThrownWithSigningNotUnlocked() {
    final AddressIndexedSignerProvider mockSignerProvider =
        mock(AddressIndexedSignerProvider.class);
    final EthSignTransactionResultProvider resultProvider =
        new EthSignTransactionResultProvider(chainId, mockSignerProvider, jsonDecoder);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTransaction");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(List.of(getTxParameters()));
    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
  }

  @Test
  public void signatureHasTheExpectedFormat() {
    final Credentials cs =
        Credentials.create("0x1618fc3e47aec7e70451256e033b9edb67f4c469258d8e2fbb105552f141ae41");
    final ECPublicKey key = EthPublicKeyUtils.createPublicKey(cs.getEcKeyPair().getPublicKey());

    final Signer mockSigner = mock(Signer.class);
    doReturn(key).when(mockSigner).getPublicKey();
    final BigInteger v = BigInteger.ONE;
    final BigInteger r = BigInteger.TWO;
    final BigInteger s = BigInteger.TEN;
    doReturn(new Signature(v, r, s)).when(mockSigner).sign(any(byte[].class));
    final AddressIndexedSignerProvider mockSignerProvider =
        mock(AddressIndexedSignerProvider.class);
    doReturn(Optional.of(mockSigner)).when(mockSignerProvider).getSigner(anyString());
    final EthSignTransactionResultProvider resultProvider =
        new EthSignTransactionResultProvider(chainId, mockSignerProvider, jsonDecoder);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTransaction");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(List.of(getTxParameters()));

    final Object result = resultProvider.createResponseResult(request);
    assertThat(result).isInstanceOf(String.class);
    final String signedTx = (String) result;
    assertThat(signedTx).hasSize(72);
  }

  @Test
  public void nonceNotProvidedExceptionIsThrownWithInvalidParams() {
    final AddressIndexedSignerProvider mockSignerProvider =
        mock(AddressIndexedSignerProvider.class);
    final EthSignTransactionResultProvider resultProvider =
        new EthSignTransactionResultProvider(chainId, mockSignerProvider, jsonDecoder);

    final JsonObject params = getTxParameters();
    params.remove("nonce");
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTransaction");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(params);
    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void returnsExpectedSignature() {
    final Credentials cs =
        Credentials.create("0x1618fc3e47aec7e70451256e033b9edb67f4c469258d8e2fbb105552f141ae41");
    final ECPublicKey key = EthPublicKeyUtils.createPublicKey(cs.getEcKeyPair().getPublicKey());
    final String addr = Keys.getAddress(EthPublicKeyUtils.toHexString(key));

    final Signer mockSigner = mock(Signer.class);
    doReturn(key).when(mockSigner).getPublicKey();

    doAnswer(
            answer -> {
              byte[] data = answer.getArgument(0, byte[].class);
              final Sign.SignatureData signature =
                  Sign.signPrefixedMessage(data, cs.getEcKeyPair());
              return new Signature(
                  new BigInteger(signature.getV()),
                  new BigInteger(1, signature.getR()),
                  new BigInteger(1, signature.getS()));
            })
        .when(mockSigner)
        .sign(any(byte[].class));
    final AddressIndexedSignerProvider mockSignerProvider =
        mock(AddressIndexedSignerProvider.class);
    doReturn(Optional.of(mockSigner)).when(mockSignerProvider).getSigner(anyString());
    final EthSignTransactionResultProvider resultProvider =
        new EthSignTransactionResultProvider(chainId, mockSignerProvider, jsonDecoder);

    final JsonObject params = getTxParameters();
    params.put("from", addr);
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTransaction");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(params);

    final Object result = resultProvider.createResponseResult(request);
    assertThat(result).isInstanceOf(String.class);
    final String encodedTransaction = (String) result;
    assertThat(encodedTransaction)
        .isEqualTo(
            "0xf862468082760094627306090abab3a6e1400e9345bc60c78a8bef57010083015e7ca0c1de8a14a6bb3882fd97d5ebc3ed6db2f15cbdf9cbd9e89027973276c9d5f6d6a068214ca6ca701eaa8e74e819f838478865c267869e362c02018a11a150422efe");
  }

  private static JsonObject getTxParameters() {
    final JsonObject jsonObject = new JsonObject();
    jsonObject.put("from", "0xf17f52151ebef6c7334fad080c5704d77216b732");
    jsonObject.put("to", "0x627306090abaB3A6e1400e9345bC60c78a8BEf57");
    jsonObject.put("gasPrice", "0x0");
    jsonObject.put("gas", "0x7600");
    jsonObject.put("nonce", "0x46");
    jsonObject.put("value", "0x1");
    jsonObject.put("data", "0x0");
    return jsonObject;
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

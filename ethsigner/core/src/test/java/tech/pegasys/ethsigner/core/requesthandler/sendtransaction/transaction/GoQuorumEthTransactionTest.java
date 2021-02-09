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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.web3j.utils.Bytes.trimLeadingZeroes;

import tech.pegasys.ethsigner.core.jsonrpc.EeaSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.EnclaveLookupIdProvider;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.protocol.eea.crypto.PrivateTransactionDecoder;
import org.web3j.protocol.eea.crypto.SignedRawPrivateTransaction;
import org.web3j.utils.Base64String;
import org.web3j.utils.Numeric;
import org.web3j.utils.Restriction;

public class GoQuorumEthTransactionTest {

  private GoQuorumPrivateTransaction ethTransaction;
  private EthSendTransactionJsonParameters params;

  @BeforeEach
  public void setup() {
    params = new EthSendTransactionJsonParameters("0x7577919ae5df4941180eac211965f275cdce314d");
    params.receiver("0xd46e8dd67c5d32be8058bb8eb970870f07244567");
    params.gas("0x76c0");
    params.gasPrice("0x9184e72a000");
    params.nonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2");
    params.value("0x0");
    params.data(
        "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");
    // These extra attributes make it a GoQuorum private transaction via eth_sendTransaction
    params.privateFrom("ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI=");
    params.privateFor(new String[] {"GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w="});

    EnclaveLookupIdProvider provider =
        () ->
            "9alPvwI5WX9Ct/1DUdNSvCdhj0bLvw+f7NZ/1oG9IaznAspXyAlqp30YzKHcx8oe+QBrnrKPldoPzy98bA7ABg==";
    ethTransaction =
        GoQuorumPrivateTransaction.from(
            new EeaSendTransactionJsonParameters(params),
            () -> BigInteger.ZERO,
            provider,
            new JsonRpcRequestId(1));
  }

  @Test
  public void eeaSendTxParamsFromEthSendTxParams() {
    EeaSendTransactionJsonParameters eeaParams = new EeaSendTransactionJsonParameters(params);
    assertThat(eeaParams.privateFrom()).isEqualTo(params.privateFrom().get());
    assertThat(eeaParams.nonce()).isEqualTo(params.nonce());
    assertThat(eeaParams.restriction()).isEqualTo("RESTRICTED");
    assertThat(eeaParams.privacyGroupId()).isEmpty();
    assertThat(eeaParams.privateFor()).isEqualTo(params.privateFor());
    assertThat(eeaParams.receiver()).isEqualTo(params.receiver());
  }

  @Test
  public void rlpEncodesTransaction() {
    final SignatureData signatureData =
        new SignatureData(new byte[] {1}, new byte[] {2}, new byte[] {3});
    final byte[] rlpEncodedBytes = ethTransaction.rlpEncode(signatureData);
    final String rlpString = Numeric.toHexString(rlpEncodedBytes);

    final SignedRawPrivateTransaction decodedTransaction =
        (SignedRawPrivateTransaction) PrivateTransactionDecoder.decode(rlpString);
    assertThat(decodedTransaction.getTo()).isEqualTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567");
    assertThat(decodedTransaction.getGasLimit()).isEqualTo(Numeric.decodeQuantity("0x76c0"));
    assertThat(decodedTransaction.getGasPrice()).isEqualTo(Numeric.decodeQuantity("0x9184e72a000"));
    assertThat(decodedTransaction.getNonce())
        .isEqualTo(
            Numeric.decodeQuantity(
                "0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2"));
    assertThat(decodedTransaction.getValue()).isEqualTo(Numeric.decodeQuantity("0x0"));
    assertThat(decodedTransaction.getData())
        .isEqualTo(
            "d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    assertThat(decodedTransaction.getRestriction()).isEqualTo(Restriction.RESTRICTED);

    final Base64String expectedDecodedPrivateFrom = params.privateFrom().get();
    final Base64String expectedDecodedPrivateFor = params.privateFor().get().get(0);

    assertThat(decodedTransaction.getPrivateFrom()).isEqualTo(expectedDecodedPrivateFrom);
    assertThat(decodedTransaction.getPrivateFor().get().get(0))
        .isEqualTo(expectedDecodedPrivateFor);

    final SignatureData decodedSignatureData = decodedTransaction.getSignatureData();
    assertThat(trimLeadingZeroes(decodedSignatureData.getV())).isEqualTo(new byte[] {1});
    assertThat(trimLeadingZeroes(decodedSignatureData.getR())).isEqualTo(new byte[] {2});
    assertThat(trimLeadingZeroes(decodedSignatureData.getS())).isEqualTo(new byte[] {3});
  }

  @Test
  @SuppressWarnings("unchecked")
  public void createsJsonRequest() {
    final JsonRpcRequestId id = new JsonRpcRequestId(2);
    final String transactionString =
        "0xf90114a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456704a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0fe72a92aede764ce41d06b163d28700b58e5ee8bb1af91d9d54979ea3bdb3e7ea046ae10c94c322fa44ddceb86677c2cd6cc17dfbd766924f41d10a244c512996dac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564";
    final JsonRpcRequest jsonRpcRequest = ethTransaction.jsonRpcRequest(transactionString, id);

    assertThat(jsonRpcRequest.getMethod()).isEqualTo("goquorum_storeRaw");
    assertThat(jsonRpcRequest.getVersion()).isEqualTo("2.0");
    assertThat(jsonRpcRequest.getId()).isEqualTo(id);
    final List<String> params = (List<String>) jsonRpcRequest.getParams();
    assertThat(params).isEqualTo(singletonList(transactionString));
  }
}

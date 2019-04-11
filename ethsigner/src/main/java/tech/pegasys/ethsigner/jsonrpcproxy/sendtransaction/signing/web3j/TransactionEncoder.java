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
package tech.pegasys.ethsigner.jsonrpcproxy.sendtransaction.signing.web3j;

import java.util.ArrayList;
import java.util.List;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

/**
 * NOTE: This was taken from Web3j commit - e1e3f346409527d485c4bb96b0d73089a90d3f2d. It has been
 * reworked such that long is encoded into the RLP stream prior to transmission to downstream Web3j
 * provider.
 */
public class TransactionEncoder {

  public static class TransmittableSignature {
    private final long v;
    private final byte[] r;
    private final byte[] s;

    public TransmittableSignature(long v, byte[] r, byte[] s) {
      this.v = v;
      this.r = r;
      this.s = s;
    }

    public long getV() {
      return v;
    }

    public byte[] getR() {
      return r;
    }

    public byte[] getS() {
      return s;
    }
  }

  public static byte[] signMessage(RawTransaction rawTransaction, Credentials credentials) {
    byte[] encodedTransaction = encode(rawTransaction);
    Sign.SignatureData signatureData =
        Sign.signMessage(encodedTransaction, credentials.getEcKeyPair());

    return encode(
        rawTransaction,
        new TransmittableSignature(
            signatureData.getV(), signatureData.getR(), signatureData.getS()));
  }

  public static byte[] signMessage(
      RawTransaction rawTransaction, long chainId, Credentials credentials) {
    byte[] encodedTransaction = encode(rawTransaction, chainId);
    Sign.SignatureData signatureData =
        Sign.signMessage(encodedTransaction, credentials.getEcKeyPair());

    TransmittableSignature eip155SignatureData = createEip155SignatureData(signatureData, chainId);
    return encode(rawTransaction, eip155SignatureData);
  }

  public static TransmittableSignature createEip155SignatureData(
      Sign.SignatureData signatureData, long chainId) {
    long v = signatureData.getV() + (chainId << 1) + 8;

    return new TransmittableSignature(v, signatureData.getR(), signatureData.getS());
  }

  public static byte[] encode(RawTransaction rawTransaction) {
    return encode(rawTransaction, null);
  }

  public static byte[] encode(RawTransaction rawTransaction, long chainId) {
    TransmittableSignature signatureData =
        new TransmittableSignature(chainId, new byte[] {}, new byte[] {});
    return encode(rawTransaction, signatureData);
  }

  private static byte[] encode(
      RawTransaction rawTransaction, TransmittableSignature signatureData) {
    List<RlpType> values = asRlpValues(rawTransaction, signatureData);
    RlpList rlpList = new RlpList(values);
    return RlpEncoder.encode(rlpList);
  }

  static List<RlpType> asRlpValues(
      RawTransaction rawTransaction, TransmittableSignature signatureData) {
    List<RlpType> result = new ArrayList<>();

    result.add(RlpString.create(rawTransaction.getNonce()));
    result.add(RlpString.create(rawTransaction.getGasPrice()));
    result.add(RlpString.create(rawTransaction.getGasLimit()));

    // an empty to address (contract creation) should not be encoded as a numeric 0 value
    String to = rawTransaction.getTo();
    if (to != null && to.length() > 0) {
      // addresses that start with zeros should be encoded with the zeros included, not
      // as numeric values
      result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
    } else {
      result.add(RlpString.create(""));
    }

    result.add(RlpString.create(rawTransaction.getValue()));

    // value field will already be hex encoded, so we need to convert into binary first
    byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
    result.add(RlpString.create(data));

    if (signatureData != null) {
      result.add(RlpString.create(signatureData.getV()));
      result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
      result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
    }

    return result;
  }
}

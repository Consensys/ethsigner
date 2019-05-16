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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.web3j.crypto.Sign.SignatureData;

public interface Transaction {

  void updateNonce(BigInteger nonce);

  byte[] rlpEncode(SignatureData signatureData);

  default byte[] rlpEncode(final long chainId) {
    final SignatureData signatureData =
        new SignatureData(longToBytes(chainId), new byte[] {}, new byte[] {});
    return rlpEncode(signatureData);
  }

  boolean hasNonce();

  String sender();

  // NOTE: This was taken from Web3j TransactionEncode as the function is private
  static byte[] longToBytes(final long x) {
    final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }
}

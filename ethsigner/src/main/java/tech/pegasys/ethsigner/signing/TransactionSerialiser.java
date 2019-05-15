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
package tech.pegasys.ethsigner.signing;

import tech.pegasys.ethsigner.requesthandler.sendtransaction.Transaction;

import java.nio.ByteBuffer;

import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class TransactionSerialiser {

  private final TransactionSigner signer;
  private final long chainId;

  public TransactionSerialiser(final TransactionSigner signer, final long chainId) {
    this.signer = signer;
    this.chainId = chainId;
  }

  public String serialise(final Transaction transaction) {
    final byte[] bytesToSign =
        transaction.rlpEncode(
            new Sign.SignatureData(longToBytes(chainId), new byte[] {}, new byte[] {}));

    final SignatureData signature = signer.sign(bytesToSign);

    final SignatureData eip155Signature =
        TransactionEncoder.createEip155SignatureData(signature, chainId);

    final byte[] serialisedBytes = transaction.rlpEncode(eip155Signature);
    return Numeric.toHexString(serialisedBytes);
  }

  /** NOTE: This was taken from Web3j TransactionEncode as the function is private */
  private static byte[] longToBytes(final long x) {
    final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }

  public String getAddress() {
    return signer.getAddress();
  }
}

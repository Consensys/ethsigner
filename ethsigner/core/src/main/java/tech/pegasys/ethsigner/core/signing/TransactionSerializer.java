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
package tech.pegasys.ethsigner.core.signing;

import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.TransactionSigner;

import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class TransactionSerializer {

  private final TransactionSigner signer;
  private final long chainId;

  public TransactionSerializer(final TransactionSigner signer, final long chainId) {
    this.signer = signer;
    this.chainId = chainId;
  }

  public String serialize(final Transaction transaction) {
    final byte[] bytesToSign = transaction.rlpEncode(chainId);

    final Signature signature = signer.sign(bytesToSign);

    final SignatureData web3jSignature =
        new SignatureData(
            signature.getV().toByteArray(),
            signature.getR().toByteArray(),
            signature.getS().toByteArray());

    final SignatureData eip155Signature =
        TransactionEncoder.createEip155SignatureData(web3jSignature, chainId);

    final byte[] serializedBytes = transaction.rlpEncode(eip155Signature);
    return Numeric.toHexString(serializedBytes);
  }

  public String getAddress() {
    return signer.getAddress();
  }
}

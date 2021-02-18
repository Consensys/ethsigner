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
import tech.pegasys.signers.secp256k1.api.Signer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

public class GoQuorumPrivateTransactionSerializer extends TransactionSerializer {

  private static final Logger LOG = LogManager.getLogger();

  public GoQuorumPrivateTransactionSerializer(Signer signer, long chainId) {
    super(signer, chainId);
  }

  @Override
  public String serialize(final Transaction transaction) {
    LOG.info("ignoring the chainId ");
    LOG.info("tx.sender " + transaction.sender());

    // TODO does signing an empty byte [] give the right thing?
    final Signature signature = signer.sign(new byte[] {});

    LOG.info("signing bytes " + " with signature V " + signature.getV());

    byte[] newV = (getGoQuorumVValue(signature.getV().toByteArray()));

    LOG.info("signing bytes " + " with signature R " + signature.getR());
    LOG.info("signing bytes " + " with signature S " + signature.getS());
    LOG.info("signing bytes " + " with NEW signature V " + newV);

    final SignatureData web3jSignature =
        new SignatureData(newV, signature.getR().toByteArray(), signature.getS().toByteArray());

    final byte[] serializedBytes = transaction.rlpEncode(web3jSignature);
    LOG.info("made serialized bytes " + Numeric.toHexString(serializedBytes));
    return Numeric.toHexString(serializedBytes);
  }

  public static byte[] getGoQuorumVValue(byte[] v) {
    LOG.info("using " + v + " to get GoQuorum V ");
    return ((v[v.length - 1] & 1) == 1) ? new byte[] {38} : new byte[] {37};
  }
}

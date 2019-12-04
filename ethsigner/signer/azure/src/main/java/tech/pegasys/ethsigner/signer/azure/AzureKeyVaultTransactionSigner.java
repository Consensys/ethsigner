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
package tech.pegasys.ethsigner.signer.azure;

import tech.pegasys.ethsigner.core.signing.Signature;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.math.BigInteger;
import java.util.Arrays;

import com.microsoft.azure.keyvault.KeyVaultClientCustom;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKeySignatureAlgorithm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

public class AzureKeyVaultTransactionSigner implements TransactionSigner {

  private static final Logger LOG = LogManager.getLogger();

  private final KeyVaultClientCustom client;
  private final String keyId;
  private final BigInteger publicKey;
  private final String address;
  private final JsonWebKeySignatureAlgorithm signingAlgo =
      new JsonWebKeySignatureAlgorithm("ECDSA256");

  public AzureKeyVaultTransactionSigner(
      final KeyVaultClientCustom client, final String keyId, final BigInteger publicKey) {
    this.client = client;
    this.keyId = keyId;
    this.publicKey = publicKey;
    this.address = "0x" + Keys.getAddress(publicKey);
  }

  @Override
  public Signature sign(final byte[] data) {
    final byte[] hash = Hash.sha3(data);
    final KeyOperationResult result = client.sign(keyId, signingAlgo, hash);
    final byte[] signature = result.result();

    if (signature.length != 64) {
      throw new RuntimeException(
          "Invalid signature from the key vault signing service, must be 64 bytes long");
    }

    // reference: blog by Tomislav Markovski
    // https://tomislav.tech/2018-02-05-ethereum-keyvault-signing-transactions/
    // The output of this will be a 64 byte array. The first 32 are the value for R and the rest is
    // S.
    final BigInteger R = new BigInteger(1, Arrays.copyOfRange(signature, 0, 32));
    final BigInteger S = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));

    // The Azure Signature MAY be in the "top" of the curve, which is illegal in Ethereum
    // thus it must be transposed to the lower intersection.
    final ECDSASignature initialSignature = new ECDSASignature(R, S);
    final ECDSASignature canonicalSignature = initialSignature.toCanonicalised();

    // Now we have to work backwards to figure out the recId needed to recover the signature.
    final int recId = recoverKeyIndex(canonicalSignature, hash);
    if (recId == -1) {
      throw new RuntimeException(
          "Could not construct a recoverable key. Are your credentials valid?");
    }

    final int headerByte = recId + 27;
    return new Signature(
        BigInteger.valueOf(headerByte), canonicalSignature.r, canonicalSignature.s);
  }

  private int recoverKeyIndex(final ECDSASignature sig, final byte[] hash) {
    for (int i = 0; i < 4; i++) {
      final BigInteger k = Sign.recoverFromSignature(i, sig, hash);
      LOG.trace("recovered key: {}", k);
      if (k != null && k.equals(publicKey)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public String getAddress() {
    return address;
  }
}

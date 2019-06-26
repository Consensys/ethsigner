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

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyIdentifier;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyItem;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import com.microsoft.azure.keyvault.webkey.JsonWebKeySignatureAlgorithm;
import java.math.BigInteger;
import java.util.MissingResourceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;
import tech.pegasys.ethsigner.core.signing.Signature;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

public class AzureKeyVaultTransactionSigner implements TransactionSigner {

  private static final Logger LOG = LogManager.getLogger();

  private final KeyVaultClient client;
  private final String keyId;
  private final KeyBundle key;
  private String address;

  private AzureKeyVaultTransactionSigner(final String keyvaultName, String keyName,
      String keyVersion) {
    this.client = AzureKeyVaultAuthenticator.getAuthenticatedClient();

    if (keyvaultName == null) {
      throw new IllegalArgumentException("KeyVault name is required");
    }

    final String baseUrl = String.format("https://%s.vault.azure.net", keyvaultName);

    // if the key name is not specified, find the first key in the specified key vault
    if (keyName == null) {
      final PagedList<KeyItem> keys = client.listKeys(baseUrl);
      if (keys.size() <= 0) {
        throw new MissingResourceException(
            String.format("No keys found in the key vault %s", keyvaultName),
            KeyItem.class.getName(), baseUrl);
      }

      final KeyItem firstKey = keys.get(0);

      // now find the latest version of that key
      final KeyIdentifier kid = new KeyIdentifier(firstKey.kid());
      LOG.info("Found the first key in the vault: {}", kid.name());
      keyName = kid.name();
    }

    // if the key version is not specified, find the latest version for the key name from the vault
    if (keyVersion == null) {
      final PagedList<KeyItem> keyVersions = client.listKeyVersions(baseUrl, keyName);
      if (keyVersions.size() <= 0) {
        throw new MissingResourceException(String
            .format("No versions found for the key %s in the key vault %s", keyName, keyvaultName),
            KeyItem.class.getName(), baseUrl);
      }

      final KeyItem latestVersion = keyVersions.get(keyVersions.size() - 1);
      final KeyIdentifier kid = new KeyIdentifier(latestVersion.kid());
      LOG.info("Found the latest version of the key {} in the vault: {}", keyName, kid.version());
      keyVersion = kid.version();
    }

    final KeyIdentifier kid = new KeyIdentifier(baseUrl, keyName, keyVersion);
    this.keyId = kid.toString();
    this.key = client.getKey(this.keyId);
  }

  public static TransactionSigner createFrom(final String keyvaultName, final String keyName,
      final String keyVersion) {
    return new AzureKeyVaultTransactionSigner(keyvaultName, keyName, keyVersion);
  }

  @Override
  public Signature sign(final byte[] data) {
    byte[] hash = Hash.sha3(data);
    final KeyOperationResult result =
        this.client.sign(this.keyId, new JsonWebKeySignatureAlgorithm("ECDSA256"), hash);
    final byte[] signature = result.result();

    if (signature.length != 64) {
      throw new RuntimeException(
          "Invalid signature from the keyvault signing service, must be 64 bytes long");
    }

    // reference: blog by Tomislav Markovski
    // https://tomislav.tech/2018-02-05-ethereum-keyvault-signing-transactions/
    // The output of this will be a 64 byte array. The first 32 are the value for R and the rest is S. 
    final byte[] R = new byte[32];
    final byte[] S = new byte[32];
    System.arraycopy(signature, 0, R, 0, 32);
    System.arraycopy(signature, 32, S, 0, 32);

    // Now we have to work backwards to figure out the recId needed to recover the signature.
    // reference: https://github.com/web3j/web3j/blob/master/crypto/src/main/java/org/web3j/crypto/Sign.java
    int recId = -1;
    final ECDSASignature sig = new ECDSASignature(Numeric.toBigInt(R), Numeric.toBigInt(S));
    final byte[] publicKeyBytes = AzureKeyVaultTransactionSigner.getRawPublicKey(this.key.key());
    final BigInteger publicKey = Numeric.toBigInt(publicKeyBytes);
    LOG.info("public key: {}", publicKey);
    for (int i = 0; i < 4; i++) {
      final BigInteger k = Sign.recoverFromSignature(i, sig, hash);
      LOG.info("recovered key: {}", k);
      if (k != null && k.equals(publicKey)) {
        recId = i;
        break;
      }
    }
    if (recId == -1) {
      throw new RuntimeException(
          "Could not construct a recoverable key. Are your credentials valid?");
    }

    int headerByte = recId + 27;

    return new Signature(BigInteger.valueOf(headerByte), new BigInteger(R), new BigInteger(S));
  }

  @Override
  public String getAddress() {
    if (this.address == null) {
      // must be built from the raw EC public key parameters retrieved from the key vault
      // reference: blog by Tomislav Markovski
      // https://tomislav.tech/2018-01-31-ethereum-keyvault-generating-keys/
      final byte[] raw = AzureKeyVaultTransactionSigner.getRawPublicKey(this.key.key());

      final byte[] hash = Hash.sha3(raw);
      // take the last 20 bytes
      final byte[] addressBytes = new byte[20];
      System.arraycopy(hash, 12, addressBytes, 0, 20);

      this.address = AzureKeyVaultTransactionSigner.bytesToHex(addressBytes);
    }
    return this.address;
  }

  private static String bytesToHex(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder("0x");
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private static byte[] getRawPublicKey(final JsonWebKey key) {
    final byte[] x = key.x();
    final byte[] y = key.y();

    final byte[] arr = new byte[x.length + y.length];
    System.arraycopy(x, 0, arr, 0, x.length);
    System.arraycopy(y, 0, arr, x.length, y.length);

    return arr;
  }
}

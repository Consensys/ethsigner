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

import static com.google.common.base.Preconditions.checkNotNull;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.math.BigInteger;
import java.util.List;

import com.google.common.primitives.Bytes;
import com.microsoft.azure.keyvault.KeyIdentifier;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyItem;
import com.microsoft.azure.keyvault.models.KeyVaultErrorException;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AzureKeyVaultTransactionSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  final String AZURE_URL_PATTERN = "https://%s.vault.azure.net";

  private final KeyVaultClient client;
  private final String baseUrl;

  public AzureKeyVaultTransactionSignerFactory(
      final String keyVaultName, final KeyVaultClient client) {
    this.client = client;
    this.baseUrl = String.format(AZURE_URL_PATTERN, keyVaultName);
  }

  public TransactionSigner createSigner(final String keyName, final String keyVersion) {
    checkNotNull(keyName, "keyName must be specified");

    final JsonWebKey key;
    final KeyIdentifier kid;
    try {
      kid = new KeyIdentifier(baseUrl, keyName, keyVersion);
      key = client.getKey(kid.toString()).key();
    } catch (final KeyVaultErrorException ex) {
      LOG.error("Unable to access key in vault ", ex);
      throw ex;
    } catch (final IllegalArgumentException ex) {
      LOG.error("Supplied key arguments failed validation.", ex);
      throw ex;
    } catch (final RuntimeException ex) {
      LOG.error("Failed to access the Azure key vault", ex);
      throw ex;
    }

    final BigInteger publicKey = new BigInteger(Bytes.concat(key.x(), key.y()));
    return new AzureKeyVaultTransactionSigner(client, kid.toString(), publicKey);
  }

  private TransactionSigner createSigner(final String keyName) {
    List<KeyItem> keyVersions = client.listKeyVersions(baseUrl, keyName);
    if (keyVersions.isEmpty()) {
      LOG.error("No versions of the specified key ({}) exist in the vault ({})", keyName, baseUrl);
      throw new IllegalArgumentException("No keys of the requested name exist in the vault.");
    }
    return createSigner(keyName, keyVersions.get(keyVersions.size() - 1).toString());
  }
}

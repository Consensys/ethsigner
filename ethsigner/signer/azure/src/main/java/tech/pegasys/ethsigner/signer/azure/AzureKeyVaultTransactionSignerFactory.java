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

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.math.BigInteger;
import java.net.UnknownHostException;

import com.google.common.primitives.Bytes;
import com.microsoft.azure.keyvault.KeyIdentifier;
import com.microsoft.azure.keyvault.KeyVaultClientCustom;
import com.microsoft.azure.keyvault.models.KeyVaultErrorException;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AzureKeyVaultTransactionSignerFactory {

  public static final String INACCESSIBLE_KEY_ERROR = "Failed to authenticate to vault.";
  public static final String INVALID_KEY_PARAMETERS_ERROR =
      "Keyvault does not contain key with specified parameters";
  public static final String INVALID_VAULT_PARAMETERS_ERROR_PATTERN =
      "Specified key vault (%s) does not exist.";
  public static final String UNKNOWN_VAULT_ACCESS_ERROR = "Failed to access the Azure key vault";

  private static final Logger LOG = LogManager.getLogger();

  private static final String AZURE_URL_PATTERN = "https://%s.vault.azure.net";
  private final AzureKeyVaultAuthenticator vaultAuthenticator;

  public AzureKeyVaultTransactionSignerFactory(
      final AzureKeyVaultAuthenticator vaultAuthenticator) {
    this.vaultAuthenticator = vaultAuthenticator;
  }

  public TransactionSigner createSigner(final AzureConfig config) {
    checkNotNull(config, "Config must be specified");
    final KeyVaultClientCustom client =
        vaultAuthenticator.getAuthenticatedClient(config.getClientId(), config.getClientSecret());
    final String baseUrl = constructAzureKeyVaultUrl(config.getKeyVaultName());

    final JsonWebKey key;
    final KeyIdentifier keyIdentifier;
    try {
      keyIdentifier = new KeyIdentifier(baseUrl, config.getKeyName(), config.getKeyVersion());
      key = client.getKey(keyIdentifier.toString()).key();
    } catch (final KeyVaultErrorException ex) {
      if (ex.response().raw().code() == 401) {
        LOG.debug(INACCESSIBLE_KEY_ERROR);
        LOG.trace(ex);
        throw new TransactionSignerInitializationException(INACCESSIBLE_KEY_ERROR, ex);
      } else {
        LOG.debug(INVALID_KEY_PARAMETERS_ERROR);
        LOG.trace(ex);
        throw new TransactionSignerInitializationException(INVALID_KEY_PARAMETERS_ERROR, ex);
      }
    } catch (final RuntimeException ex) {
      final String errorMsg;
      if (ex.getCause() instanceof UnknownHostException) {
        errorMsg = String.format(INVALID_VAULT_PARAMETERS_ERROR_PATTERN, baseUrl);
      } else {
        errorMsg = UNKNOWN_VAULT_ACCESS_ERROR;
      }
      LOG.debug(errorMsg);
      LOG.trace(ex);
      throw new TransactionSignerInitializationException(errorMsg, ex);
    }

    final byte[] rawPublicKey = Bytes.concat(key.x(), key.y());
    final BigInteger publicKey = new BigInteger(1, rawPublicKey);
    return new AzureKeyVaultTransactionSigner(client, keyIdentifier.toString(), publicKey);
  }

  public static String constructAzureKeyVaultUrl(final String keyVaultName) {
    return String.format(AZURE_URL_PATTERN, keyVaultName);
  }
}

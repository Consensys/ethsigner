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

import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import com.microsoft.azure.keyvault.KeyVaultClientCustom;

public class Helpers {

  public static TransactionSigner createSigner(final AzureConfig config) {
    final KeyVaultClientCustom client =
        createKeyVaultClient(config.getClientId(), config.getClientSecret());
    final AzureKeyVaultTransactionSignerFactory factory =
        new AzureKeyVaultTransactionSignerFactory(config.getKeyvaultName(), client);

    return factory.createSigner(config.getKeyName(), config.getKeyVersion());
  }

  private static KeyVaultClientCustom createKeyVaultClient(
      final String clientId, final String clientSecret) {
    final AzureKeyVaultAuthenticator authenticator = new AzureKeyVaultAuthenticator();
    return authenticator.getAuthenticatedClient(clientId, clientSecret);
  }
}

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
package tech.pegasys.ethsigner.signer.multiplatform;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultAuthenticator;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultTransactionSignerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiPlatformTransactionSignerProvider implements TransactionSignerProvider {

  private static final Logger LOG = LogManager.getLogger();

  private final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader;
  private final MultiSignerFactory signerFactory;

  MultiPlatformTransactionSignerProvider(
      final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader) {
    this.signingMetadataTomlConfigLoader = signingMetadataTomlConfigLoader;

    final AzureKeyVaultAuthenticator azureAuthenticator = new AzureKeyVaultAuthenticator();
    final AzureKeyVaultTransactionSignerFactory azureFactory =
        new AzureKeyVaultTransactionSignerFactory(azureAuthenticator);

    signerFactory = new MultiSignerFactory(azureFactory);
  }

  @Override
  public Optional<TransactionSigner> getSigner(final String address) {
    return signingMetadataTomlConfigLoader
        .loadMetadataForAddress(address)
        .map(metaDataFile -> metaDataFile.createSigner(signerFactory));
  }

  @Override
  public Set<String> availableAddresses() {
    return signingMetadataTomlConfigLoader.loadAvailableSigningMetadataTomlConfigs().stream()
        .map(metaDataFile -> metaDataFile.createSigner(signerFactory))
        .filter(Objects::nonNull)
        .map(TransactionSigner::getAddress)
        .collect(Collectors.toSet());
  }
}

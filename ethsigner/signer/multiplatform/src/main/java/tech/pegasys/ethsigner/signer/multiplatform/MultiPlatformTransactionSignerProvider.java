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

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultAuthenticator;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultTransactionSignerFactory;
import tech.pegasys.ethsigner.signer.filebased.FileBasedSignerFactory;
import tech.pegasys.ethsigner.signer.multiplatform.metadata.AzureSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multiplatform.metadata.FileBasedSigningMetadataFile;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiPlatformTransactionSignerProvider
    implements TransactionSignerProvider, MultiSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  private final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader;
  private final AzureKeyVaultTransactionSignerFactory azureFactory;

  MultiPlatformTransactionSignerProvider(
      final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader) {
    this.signingMetadataTomlConfigLoader = signingMetadataTomlConfigLoader;

    final AzureKeyVaultAuthenticator azureAuthenticator = new AzureKeyVaultAuthenticator();
    azureFactory = new AzureKeyVaultTransactionSignerFactory(azureAuthenticator);
  }

  @Override
  public Optional<TransactionSigner> getSigner(final String address) {
    return signingMetadataTomlConfigLoader
        .loadMetadataForAddress(address)
        .map(metaDataFile -> metaDataFile.createSigner(this));
  }

  @Override
  public Set<String> availableAddresses() {
    return signingMetadataTomlConfigLoader.loadAvailableSigningMetadataTomlConfigs().stream()
        .map(metaDataFile -> metaDataFile.createSigner(this))
        .filter(Objects::nonNull)
        .map(TransactionSigner::getAddress)
        .collect(Collectors.toSet());
  }

  @Override
  public TransactionSigner createSigner(final AzureSigningMetadataFile metaDataFile) {
    final TransactionSigner signer;
    try {
      signer = azureFactory.createSigner(metaDataFile.getConfig());
    } catch (final TransactionSignerInitializationException e) {
      LOG.error("Failed to construct azure siger from {}", metaDataFile.getBaseFilename());
      return null;
    }

    final String signerAddress = signer.getAddress();
    if (!signerAddress.equals(metaDataFile.getBaseFilename())) {
      LOG.error(
          "Azure Signer's Ethereum Address ({}) does not align with metadata filename ({})",
          signerAddress,
          metaDataFile.getBaseFilename());
      throw new IllegalArgumentException("Mismatch between signer and filename.");
    }
    LOG.info("Loaded signer for address {}", signerAddress);
    return signer;
  }

  @Override
  public TransactionSigner createSigner(final FileBasedSigningMetadataFile signingMetadataFile) {
    try {
      final TransactionSigner signer =
          FileBasedSignerFactory.createSigner(
              signingMetadataFile.getKeyPath(), signingMetadataFile.getPasswordPath());
      LOG.debug("Loaded signer with key '{}'", signingMetadataFile.getKeyPath().getFileName());
      return signer;
    } catch (final TransactionSignerInitializationException e) {
      LOG.warn(
          "Unable to load signer with key '{}'", signingMetadataFile.getKeyPath().getFileName(), e);
      return null;
    }
  }
}

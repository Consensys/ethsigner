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
package tech.pegasys.ethsigner.signer.multikey;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultTransactionSignerFactory;
import tech.pegasys.ethsigner.signer.filebased.FileBasedSignerFactory;
import tech.pegasys.ethsigner.signer.hashicorp.HashicorpSignerFactory;
import tech.pegasys.ethsigner.signer.multikey.metadata.AzureSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.HashicorpSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.SigningMetadataFile;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiKeyTransactionSignerProvider
    implements TransactionSignerProvider, MultiSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  private final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader;
  private final AzureKeyVaultTransactionSignerFactory azureFactory;
  private final HashicorpSignerFactory hashicorpFactory;

  MultiKeyTransactionSignerProvider(
      final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader,
      final AzureKeyVaultTransactionSignerFactory azureFactory,
      final HashicorpSignerFactory hashicorpFactory) {
    this.signingMetadataTomlConfigLoader = signingMetadataTomlConfigLoader;
    this.azureFactory = azureFactory;
    this.hashicorpFactory = hashicorpFactory;
  }

  @Override
  public Optional<TransactionSigner> getSigner(final String address) {
    return signingMetadataTomlConfigLoader
        .loadMetadataForAddress(address)
        .map(metadataFile -> metadataFile.createSigner(this));
  }

  @Override
  public Set<String> availableAddresses() {
    return signingMetadataTomlConfigLoader.loadAvailableSigningMetadataTomlConfigs().stream()
        .map(metadataFile -> metadataFile.createSigner(this))
        .filter(Objects::nonNull)
        .map(TransactionSigner::getAddress)
        .collect(Collectors.toSet());
  }

  @Override
  public TransactionSigner createSigner(final AzureSigningMetadataFile metadataFile) {
    final TransactionSigner signer;
    try {
      signer = azureFactory.createSigner(metadataFile.getConfig());
    } catch (final TransactionSignerInitializationException e) {
      LOG.error("Failed to construct Azure signer from " + metadataFile.getBaseFilename());
      return null;
    }

    if (filenameMatchesSigningAddress(signer.getAddress(), metadataFile)) {
      LOG.info("Loaded signer for address {}", signer.getAddress());
      return signer;
    }

    return null;
  }

  @Override
  public TransactionSigner createSigner(final HashicorpSigningMetadataFile metadataFile) {
    final TransactionSigner signer;
    try {
      signer = hashicorpFactory.createSigner(metadataFile.getConfig());
    } catch (final TransactionSignerInitializationException e) {
      LOG.error("Failed to construct Hashicorp signer from " + metadataFile.getBaseFilename());
      return null;
    }

    if (filenameMatchesSigningAddress(signer.getAddress(), metadataFile)) {
      LOG.info("Loaded signer for address {}", signer.getAddress());
      return signer;
    }

    return null;
  }

  @Override
  public TransactionSigner createSigner(final FileBasedSigningMetadataFile metadataFile) {
    try {
      final TransactionSigner signer =
          FileBasedSignerFactory.createSigner(
              metadataFile.getKeyPath(), metadataFile.getPasswordPath());
      final String signerAddress = signer.getAddress().substring(2); // strip leading 0x
      if (filenameMatchesSigningAddress(signerAddress, metadataFile)) {
        LOG.info("Loaded signer for address {}", signer.getAddress());
        return signer;
      }

      return null;

    } catch (final TransactionSignerInitializationException e) {
      LOG.error("Unable to load signer with key " + metadataFile.getKeyPath().getFileName(), e);
      return null;
    }
  }

  private boolean filenameMatchesSigningAddress(
      final String signerAddress, final SigningMetadataFile metadataFile) {

    if (!metadataFile.getBaseFilename().endsWith(signerAddress)) {
      LOG.error(
          String.format(
              "Signer's Ethereum Address (%s) does not align with metadata filename (%s)",
              signerAddress, metadataFile.getBaseFilename()));
      return false;
    }
    return true;
  }
}

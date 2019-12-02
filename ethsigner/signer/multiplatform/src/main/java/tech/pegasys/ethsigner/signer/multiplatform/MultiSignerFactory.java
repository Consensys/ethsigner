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
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultTransactionSignerFactory;
import tech.pegasys.ethsigner.signer.filebased.FileBasedSignerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  final AzureKeyVaultTransactionSignerFactory azureFactory;

  public MultiSignerFactory(AzureKeyVaultTransactionSignerFactory azureFactory) {
    this.azureFactory = azureFactory;
  }

  public TransactionSigner createSigner(final AzureSigningMetadataFile metaData) {
    final TransactionSigner signer = azureFactory.createSigner(metaData.getConfig());

    // assert signer.getAddress() == metaData.getFilename();

    return signer;
  }

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

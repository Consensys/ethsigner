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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.ethsigner.signer.multikey.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.ethsigner.signer.multikey.MetadataFileFixture.LOWERCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multikey.MetadataFileFixture.copyMetadataFileToDirectory;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultAuthenticator;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultTransactionSignerFactory;
import tech.pegasys.ethsigner.signer.multikey.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.SigningMetadataFile;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiKeyTransactionSignerProviderTest {

  @TempDir Path configsDirectory;

  private SigningMetadataTomlConfigLoader loader = mock(SigningMetadataTomlConfigLoader.class);
  final AzureKeyVaultTransactionSignerFactory azureFactory =
      new AzureKeyVaultTransactionSignerFactory(new AzureKeyVaultAuthenticator());
  private MultiKeyTransactionSignerProvider signerFactory =
      new MultiKeyTransactionSignerProvider(loader, azureFactory, null);
  private FileBasedSigningMetadataFile metadataFile;
  private final String KEY_FILENAME = "k.key";
  private final String PASSWORD_FILENAME = "p.password";

  @BeforeEach
  void beforeEach() {
    final Path newKeyFile = configsDirectory.resolve(KEY_FILENAME);
    final Path newPasswordFile = configsDirectory.resolve(PASSWORD_FILENAME);

    metadataFile =
        copyMetadataFileToDirectory(
            configsDirectory,
            LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION,
            newKeyFile.toAbsolutePath().toString(),
            newPasswordFile.toAbsolutePath().toString());

    // make sure the password files are where they are expected to be
    try {
      Files.copy(
          Path.of(Resources.getResource("metadata-toml-configs").toURI()).resolve(KEY_FILENAME),
          newKeyFile);
      Files.copy(
          Path.of(Resources.getResource("metadata-toml-configs").toURI())
              .resolve(PASSWORD_FILENAME),
          newPasswordFile);
    } catch (Exception e) {
      fail("Error copying metadata files", e);
    }
  }

  @Test
  void getSignerForAvailableMetadataReturnsSigner() {
    when(loader.loadMetadataForAddress(LOWERCASE_ADDRESS)).thenReturn(Optional.of(metadataFile));

    final Optional<TransactionSigner> signer = signerFactory.getSigner(LOWERCASE_ADDRESS);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getAddress()).isEqualTo("0x" + LOWERCASE_ADDRESS);
  }

  @Test
  void getAddresses() {
    final Collection<SigningMetadataFile> files = Collections.singleton(metadataFile);
    when(loader.loadAvailableSigningMetadataTomlConfigs()).thenReturn(files);
    assertThat(signerFactory.availableAddresses()).containsExactly("0x" + LOWERCASE_ADDRESS);
  }

  @Test
  void signerIsLoadedSuccessfullyWhenAddressHasCaseMismatchToFilename() throws URISyntaxException {
    final FileBasedSigningMetadataFile capitalisedMetadata =
        new FileBasedSigningMetadataFile(
            LOWERCASE_ADDRESS.toUpperCase() + ".toml",
            Path.of(Resources.getResource("metadata-toml-configs").toURI()).resolve(KEY_FILENAME),
            Path.of(Resources.getResource("metadata-toml-configs").toURI())
                .resolve(PASSWORD_FILENAME));

    final TransactionSigner signer = signerFactory.createSigner(capitalisedMetadata);
    assertThat(signer).isNotNull();
    assertThat(capitalisedMetadata.getBaseFilename())
        .isNotEqualTo(signer.getAddress().substring(2));
    assertThat(signer.getAddress()).isEqualTo("0x" + LOWERCASE_ADDRESS);
  }
}

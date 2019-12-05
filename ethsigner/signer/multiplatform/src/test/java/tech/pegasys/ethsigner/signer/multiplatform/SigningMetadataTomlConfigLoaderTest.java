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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.KEY_FILE_2;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.LOWERCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_KEY_AND_PASSWORD_PATH_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_KEY_AND_PASSWORD_PATH_FILENAME;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_KEY_PATH_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_KEY_PATH_FILENAME;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_PASSWORD_PATH_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_PASSWORD_PATH_FILENAME;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PASSWORD_FILE_2;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_FILENAME_1;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_FILENAME_2;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_MIXEDCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_MIXEDCASE_FILENAME;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.SUFFIX_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.UNKNOWN_TYPE_SIGNER_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.UNKNOWN_TYPE_SIGNER_FILENAME;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.copyMetadataFileToDirectory;

import tech.pegasys.ethsigner.signer.multiplatform.metadata.AzureSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multiplatform.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multiplatform.metadata.SigningMetadataFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SigningMetadataTomlConfigLoaderTest {

  @TempDir Path configsDirectory;

  private SigningMetadataTomlConfigLoader loader;

  @BeforeEach
  void beforeEach() {
    loader = new SigningMetadataTomlConfigLoader(configsDirectory);
  }

  @Test
  void loadMetadataFilePopulatesKeyAndPasswordFilePaths() {

    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        copyMetadataFileToDirectory(
            configsDirectory, LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(LOWERCASE_ADDRESS);

    assertThat(loadedMetadataFile).isNotEmpty();
    final FileBasedSigningMetadataFile fileBasedSigningMetadata =
        (FileBasedSigningMetadataFile) loadedMetadataFile.get();
    assertThat(fileBasedSigningMetadata.getKeyPath())
        .isEqualTo(fileBasedSigningMetadataFile.getKeyPath());
    assertThat(fileBasedSigningMetadata.getPasswordPath())
        .isEqualTo(fileBasedSigningMetadataFile.getPasswordPath());
  }

  @Test
  void loadMetadataFileWithMixedCaseFilenamePopulatesKeyAndPasswordFilePaths() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        copyMetadataFileToDirectory(
            configsDirectory, PREFIX_MIXEDCASE_FILENAME, KEY_FILE, PASSWORD_FILE);

    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(PREFIX_MIXEDCASE_ADDRESS);

    assertThat(loadedMetadataFile).isNotEmpty();
    final FileBasedSigningMetadataFile fileBasedSigningMetadata =
        (FileBasedSigningMetadataFile) loadedMetadataFile.get();
    assertThat(fileBasedSigningMetadata.getKeyPath())
        .isEqualTo(fileBasedSigningMetadataFile.getKeyPath());
    assertThat(fileBasedSigningMetadata.getPasswordPath())
        .isEqualTo(fileBasedSigningMetadataFile.getPasswordPath());
  }

  @Test
  void loadMetadataFileWithUnknownTypeSignerFails() {
    copyMetadataFileToDirectory(
        configsDirectory, UNKNOWN_TYPE_SIGNER_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(UNKNOWN_TYPE_SIGNER_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingKeyPathIsEmpty() {
    copyMetadataFileToDirectory(
        configsDirectory, MISSING_KEY_PATH_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(MISSING_KEY_PATH_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingPasswordPathIsEmpty() {
    copyMetadataFileToDirectory(
        configsDirectory, MISSING_PASSWORD_PATH_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(MISSING_PASSWORD_PATH_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithHexPrefixReturnsFile() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        copyMetadataFileToDirectory(
            configsDirectory, LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress("0x" + LOWERCASE_ADDRESS);

    assertThat(loadedMetadataFile).isNotEmpty();
    final FileBasedSigningMetadataFile fileBasedSigningMetadata =
        (FileBasedSigningMetadataFile) loadedMetadataFile.get();
    assertThat(fileBasedSigningMetadata.getKeyPath())
        .isEqualTo(fileBasedSigningMetadataFile.getKeyPath());
    assertThat(fileBasedSigningMetadata.getPasswordPath())
        .isEqualTo(fileBasedSigningMetadataFile.getPasswordPath());
  }

  @Test
  void loadMetadataFileWithMissingKeyAndPasswordPathIsEmpty() {
    copyMetadataFileToDirectory(
        configsDirectory, MISSING_KEY_AND_PASSWORD_PATH_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(MISSING_KEY_AND_PASSWORD_PATH_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void multipleMatchesForSameAddressReturnsEmpty() {
    copyMetadataFileToDirectory(
        configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_2, KEY_FILE, PASSWORD_FILE);
    copyMetadataFileToDirectory(
        configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_1, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(PREFIX_LOWERCASE_DUPLICATE_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadKeyPasswordNotEndingWithAddressReturnsEmpty() {
    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(SUFFIX_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadAvailableConfigsReturnsAllValidMetadataFilesInDirectory() {
    final FileBasedSigningMetadataFile metadataFile1 =
        copyMetadataFileToDirectory(
            configsDirectory, LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);
    final FileBasedSigningMetadataFile metadataFile2 =
        copyMetadataFileToDirectory(
            configsDirectory, PREFIX_MIXEDCASE_FILENAME, KEY_FILE, PASSWORD_FILE);
    final FileBasedSigningMetadataFile metadataFile3 =
        copyMetadataFileToDirectory(
            configsDirectory,
            "bar_" + PREFIX_ADDRESS + CONFIG_FILE_EXTENSION,
            KEY_FILE_2,
            PASSWORD_FILE_2);
    final FileBasedSigningMetadataFile metadataFile4 =
        copyMetadataFileToDirectory(
            configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_2, KEY_FILE_2, PASSWORD_FILE_2);
    final FileBasedSigningMetadataFile metadataFile5 =
        copyMetadataFileToDirectory(
            configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_1, KEY_FILE, PASSWORD_FILE);

    // duplicate files are loaded at this stage since addresses aren't checked until signers are
    // created
    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles).hasSize(5);
    assertThat(metadataFiles)
        .containsOnly(metadataFile1, metadataFile2, metadataFile3, metadataFile4, metadataFile5);
  }

  private void copyFileIntoConfigDirectory(final String filename) {
    final Path newMetadataFile = configsDirectory.resolve(filename);

    try {
      Files.copy(
          Path.of(Resources.getResource("metadata-toml-configs").toURI()).resolve(filename),
          newMetadataFile);
    } catch (Exception e) {
      fail("Error copying metadata files", e);
    }
  }

  @Test
  void azureConfigIsLoadedIfAzureMetadataFileInDirectory() {
    copyFileIntoConfigDirectory("azureconfig.toml");

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(AzureSigningMetadataFile.class);
    final AzureSigningMetadataFile metadataFile =
        (AzureSigningMetadataFile) metadataFiles.toArray()[0];

    assertThat(metadataFile.getConfig().getClientId()).isEqualTo("theClientId");
    assertThat(metadataFile.getConfig().getClientSecret()).isEqualTo("theClientSecret");
    assertThat(metadataFile.getConfig().getKeyVaultName()).isEqualTo("ethsignertestkey");
    assertThat(metadataFile.getConfig().getKeyName()).isEqualTo("TestKey");
    assertThat(metadataFile.getConfig().getKeyVersion())
        .isEqualTo("7c01fe58d68148bba5824ce418241092");
  }

  @Test
  void azureConfigWithIllegalValueTypeFailsToLoad() {
    copyFileIntoConfigDirectory("azureconfig_illegalValueType.toml");
    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isZero();
  }

  @Test
  void azureConfigWithMissingFieldFailsToLoad() {
    copyFileIntoConfigDirectory("azureconfig_missingField.toml");

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isZero();
  }
}

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
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_KEY_AND_PASSWORD_PATH_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_KEY_PATH_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.MISSING_PASSWORD_PATH_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.NO_PREFIX_LOWERCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_ADDRESS_UNKNOWN_TYPE_SIGNER;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_MIXEDCASE_KP;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.SUFFIX_KP_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.load;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SigningMetadataTomlConfigLoaderTest {

  private SigningMetadataTomlConfigLoader loader;

  private static final Path metadataTomlConfigsDirectory =
      Path.of("src/test/resources/metadata-toml-configs");

  @BeforeEach
  void beforeEach() {
    loader = new SigningMetadataTomlConfigLoader(metadataTomlConfigsDirectory);
  }

  @Test
  void loadMetadataFilePopulatesKeyAndPasswordFilePaths() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(NO_PREFIX_LOWERCASE_ADDRESS, KEY_FILE, PASSWORD_FILE);

    final Optional<FileBasedSigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(NO_PREFIX_LOWERCASE_ADDRESS);

    assertThat(loadedMetadataFile).isNotEmpty();
    assertThat(loadedMetadataFile.get().getKeyPath())
        .isEqualTo(fileBasedSigningMetadataFile.getKeyPath());
    assertThat(loadedMetadataFile.get().getPasswordPath())
        .isEqualTo(fileBasedSigningMetadataFile.getPasswordPath());
  }

  @Test
  void loadMetadataFileWithMixedCaseFilenamePopulatesKeyAndPasswordFilePaths() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(PREFIX_MIXEDCASE_KP, KEY_FILE, PASSWORD_FILE);

    final Optional<FileBasedSigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(PREFIX_MIXEDCASE_KP);

    assertThat(loadedMetadataFile).isNotEmpty();
    assertThat(loadedMetadataFile.get().getKeyPath())
        .isEqualTo(fileBasedSigningMetadataFile.getKeyPath());
    assertThat(loadedMetadataFile.get().getPasswordPath())
        .isEqualTo(fileBasedSigningMetadataFile.getPasswordPath());
  }

  @Test
  void loadMetadataFileWithUnknownTypeSignerFails() {
    final Optional<FileBasedSigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(PREFIX_ADDRESS_UNKNOWN_TYPE_SIGNER);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingKeyPathIsEmpty() {
    final Optional<FileBasedSigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(MISSING_KEY_PATH_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingPasswordPathIsEmpty() {
    final Optional<FileBasedSigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(MISSING_PASSWORD_PATH_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingKeyAndPasswordPathIsEmpty() {
    final Optional<FileBasedSigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(MISSING_KEY_AND_PASSWORD_PATH_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadKeyPasswordNotEndingWithAddressReturnsEmpty() {
    final Optional<FileBasedSigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(SUFFIX_KP_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }
}

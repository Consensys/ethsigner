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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.LOWERCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.load;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FileBasedSigningMetadataFileTest {

  @Test
  void matchingMetadataFileWithoutPrefixShouldHaveExpectedName() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

    assertThat(fileBasedSigningMetadataFile.getFilename()).matches(LOWERCASE_ADDRESS);
    assertThat(fileBasedSigningMetadataFile.getKeyPath().toFile().toString()).matches(KEY_FILE);
    assertThat(fileBasedSigningMetadataFile.getPasswordPath().toFile().toString())
        .matches(PASSWORD_FILE);
  }

  @Test
  void matchingMetadataFileWithPrefixShouldHaveExpectedName() {
    final String prefix = "bar_";
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(prefix + PREFIX_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

    assertThat(fileBasedSigningMetadataFile.getFilename()).matches(prefix + PREFIX_ADDRESS);
    assertThat(fileBasedSigningMetadataFile.getKeyPath().toFile().toString()).matches(KEY_FILE);
    assertThat(fileBasedSigningMetadataFile.getPasswordPath().toFile().toString())
        .matches(PASSWORD_FILE);
  }

  @Test
  void keyWithInvalidExtensionThrowsIllegalArgumentException() {
    final Path metadataFile = Path.of("invalid_extension.txt");
    final Path keyFile = Path.of("valid_extension.key");
    final Path passwordFile = Path.of("valid_extension.password");

    assertThatThrownBy(
            () -> {
              new FileBasedSigningMetadataFile(
                  metadataFile.getFileName().toString(), keyFile, passwordFile);
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid TOML config filename extension");
  }
}

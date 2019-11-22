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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.NO_PREFIX_LOWERCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PREFIX_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.load;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FileBasedSigningMetadataFileTest {

  @Test
  void matchingMetadataFileWithoutPrefixShouldHaveExpectedName() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(NO_PREFIX_LOWERCASE_ADDRESS, KEY_FILE, PASSWORD_FILE);

    assertThat(fileBasedSigningMetadataFile.getFilename()).matches(NO_PREFIX_LOWERCASE_ADDRESS);
    assertThat(fileBasedSigningMetadataFile.getKey().toFile().toString()).matches(KEY_FILE);
    assertThat(fileBasedSigningMetadataFile.getPassword().toFile().toString())
        .matches(PASSWORD_FILE);
  }

  @Test
  void matchingMetadataFileWithPrefixShouldHaveExpectedName() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(PREFIX_ADDRESS, KEY_FILE, PASSWORD_FILE);

    assertThat(fileBasedSigningMetadataFile.getFilename()).matches(PREFIX_ADDRESS);
    assertThat(fileBasedSigningMetadataFile.getKey().toFile().toString()).matches(KEY_FILE);
    assertThat(fileBasedSigningMetadataFile.getPassword().toFile().toString())
        .matches(PASSWORD_FILE);
  }

  @Test
  void keyWithInvalidExtensionThrowsIllegalArgumentException() {
    final Path metadataFile = Path.of("invalid_extension.txt");
    final Path keyFile = Path.of("valid_extension.key");
    final Path passwordFile = Path.of("valid_extension.password");

    final IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new FileBasedSigningMetadataFile(metadataFile, keyFile, passwordFile));
    assertThat(thrown.getMessage()).isEqualTo("Invalid TOML config filename extension");
  }
}

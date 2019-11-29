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
package tech.pegasys.ethsigner.signer.multifilebased;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.NO_PREFIX_LOWERCASE_KP;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.PREFIX_MIXEDCASE_KP;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.loadKeyPasswordFile;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class KeyPasswordFileTest {

  @Test
  void matchingKeyAndPasswordWithoutPrefixShouldHaveExpectedName() {
    final KeyPasswordFile kpFile = loadKeyPasswordFile(NO_PREFIX_LOWERCASE_KP);

    assertThat(kpFile.getFilename()).matches(NO_PREFIX_LOWERCASE_KP);
  }

  @Test
  void matchingKeyAndPasswordWithPrefixShouldHaveExpectedName() {
    final KeyPasswordFile kpFile = loadKeyPasswordFile(PREFIX_MIXEDCASE_KP);

    assertThat(kpFile.getFilename()).matches(PREFIX_MIXEDCASE_KP);
  }

  @Test
  void nonMatchingKeyAndPasswordThrowsIllegalArgumentException() {
    final Path keyFile = Path.of("foo.key");
    final Path passwordFile = Path.of("bar.password");

    final IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> new KeyPasswordFile(keyFile, passwordFile));
    assertThat(thrown.getMessage()).isEqualTo("Key and Password names must match");
  }

  @Test
  void keyWithInvalidExtensionThrowsIllegalArgumentException() {
    final Path keyFile = Path.of("invalid_extension.txt");
    final Path passwordFile = Path.of("valid_extension.password");

    final IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> new KeyPasswordFile(keyFile, passwordFile));
    assertThat(thrown.getMessage()).isEqualTo("Invalid key/password filename extension");
  }

  @Test
  void passwordWithInvalidExtensionThrowsIllegalArgumentException() {
    final Path keyFile = Path.of("valid_extension.key");
    final Path passwordFile = Path.of("invalid_extension.txt");

    final IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> new KeyPasswordFile(keyFile, passwordFile));
    assertThat(thrown.getMessage()).isEqualTo("Invalid key/password filename extension");
  }
}

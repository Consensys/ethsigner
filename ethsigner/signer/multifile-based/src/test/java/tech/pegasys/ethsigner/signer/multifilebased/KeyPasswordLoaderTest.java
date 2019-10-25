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
import static org.junit.jupiter.api.Assertions.fail;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.NO_PREFIX_LOWERCASE_KP;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.NO_PREFIX_LOWERCASE_KP_ADDRESS;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.PREFIX_MIXEDCASE_KP;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.PREFIX_MIXEDCASE_KP_ADDRESS;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.loadKeyPasswordFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeyPasswordLoaderTest {

  @TempDir Path keysDirectory;
  KeyPasswordLoader loader;

  @BeforeEach
  void beforeEach() {
    loader = new KeyPasswordLoader(keysDirectory);
  }

  @Test
  void loadKeyPasswordWithMatchingPasswordReturnsFile() {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress(NO_PREFIX_LOWERCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isNotEmpty();
    assertThat(loadedKeyPassFile.get()).isEqualTo(keyPasswordFile);
  }

  @Test
  void loadKeyPasswordWithPrefixReturnsFile() {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(PREFIX_MIXEDCASE_KP);
    final String keyFilename = keyPasswordFile.getKey().getFileName().toString();
    assertThat(keyFilename.startsWith(PREFIX_MIXEDCASE_KP_ADDRESS)).isFalse();

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress(PREFIX_MIXEDCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isNotEmpty();
    assertThat(loadedKeyPassFile.get()).isEqualTo(keyPasswordFile);
  }

  @Test
  void loadKeyPasswordWithMissingPasswordReturnsEmpty() throws IOException {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    Files.delete(keyPasswordFile.getPassword());

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress(NO_PREFIX_LOWERCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isEmpty();
  }

  @Test
  void loadKeyPasswordWithMissingKeyReturnsEmpty() throws IOException {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    Files.delete(keyPasswordFile.getKey());

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress(NO_PREFIX_LOWERCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isEmpty();
  }

  @Test
  void loadKeyPasswordWithMissingKeyAndPasswordReturnsEmpty() throws IOException {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    Files.delete(keyPasswordFile.getKey());
    Files.delete(keyPasswordFile.getPassword());

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress(NO_PREFIX_LOWERCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isEmpty();
  }

  @Test
  void loadKeyPasswordWorkWithNonLowercaseFilename() throws IOException {
    final KeyPasswordFile kpFile = copyKeyPasswordToKeysDirectory(PREFIX_MIXEDCASE_KP);
    renameFile(kpFile.getKey(), PREFIX_MIXEDCASE_KP.toUpperCase() + ".key");
    renameFile(kpFile.getPassword(), PREFIX_MIXEDCASE_KP.toUpperCase() + ".password");

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress(PREFIX_MIXEDCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isNotEmpty();
  }

  @Test
  void loadAvailableKeysReturnsAllValidKeyPasswordFilesInKeysDirectory() {
    final KeyPasswordFile kpFile1 = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    final KeyPasswordFile kpFile2 = copyKeyPasswordToKeysDirectory(PREFIX_MIXEDCASE_KP);

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).hasSize(2);
    assertThat(keyPasswordFiles).containsOnly(kpFile1, kpFile2);
  }

  @Test
  void loadAvailableKeysIgnoresKeyWithoutMatchingPassword() throws IOException {
    final KeyPasswordFile kpFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    Files.delete(kpFile.getPassword());

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).isEmpty();
  }

  @Test
  void loadAvailableKeysIgnoresPasswordWithoutMatchingKey() throws IOException {
    final KeyPasswordFile kpFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    Files.delete(kpFile.getKey());

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).isEmpty();
  }

  @Test
  void loadAvailableKeysKeysWorkWithNonLowercaseFilename() throws IOException {
    final KeyPasswordFile originalKpFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    final Path renamedKey =
        renameFile(originalKpFile.getKey(), NO_PREFIX_LOWERCASE_KP.toUpperCase() + ".key");
    final Path renamedPassword =
        renameFile(
            originalKpFile.getPassword(), NO_PREFIX_LOWERCASE_KP.toUpperCase() + ".password");
    final KeyPasswordFile expectedKeyPassword = new KeyPasswordFile(renamedKey, renamedPassword);

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).hasSize(1);
    assertThat(keyPasswordFiles).containsOnly(expectedKeyPassword);
  }

  @Test
  void loadKeyPasswordWithHexPrefixReturnsFile() {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress("0x" + NO_PREFIX_LOWERCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isNotEmpty();
    assertThat(loadedKeyPassFile.get()).isEqualTo(keyPasswordFile);
  }

  @Test
  void multipleMatchesForSameAddressReturnsEmpty() throws IOException {
    final KeyPasswordFile keyPasswordFile1 = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    renameFile(keyPasswordFile1.getKey(), "foo_" + NO_PREFIX_LOWERCASE_KP_ADDRESS + ".key");
    renameFile(
        keyPasswordFile1.getPassword(), "foo_" + NO_PREFIX_LOWERCASE_KP_ADDRESS + ".password");

    final KeyPasswordFile keyPasswordFile2 = copyKeyPasswordToKeysDirectory(NO_PREFIX_LOWERCASE_KP);
    renameFile(keyPasswordFile2.getKey(), "bar_" + NO_PREFIX_LOWERCASE_KP_ADDRESS + ".key");
    renameFile(
        keyPasswordFile2.getPassword(), "bar_" + NO_PREFIX_LOWERCASE_KP_ADDRESS + ".password");

    final Optional<KeyPasswordFile> loadedKeyPassFile =
        loader.loadKeyAndPasswordForAddress(NO_PREFIX_LOWERCASE_KP_ADDRESS);

    assertThat(loadedKeyPassFile).isEmpty();
  }

  private KeyPasswordFile copyKeyPasswordToKeysDirectory(final String filename) {
    final KeyPasswordFile kpFile = loadKeyPasswordFile(filename);
    final Path newKeyFile = keysDirectory.resolve(kpFile.getKey().getFileName());
    final Path newPasswordFile = keysDirectory.resolve(kpFile.getPassword().getFileName());

    try {
      Files.copy(kpFile.getKey(), newKeyFile);
      Files.copy(kpFile.getPassword(), newPasswordFile);
    } catch (IOException e) {
      fail("Error copying key/password files", e);
    }

    return new KeyPasswordFile(newKeyFile, newPasswordFile);
  }

  private Path renameFile(final Path file, final String newName) throws IOException {
    final Path newFile = file.getParent().resolve(newName);
    Files.move(file, newFile);
    assertThat(newFile.toFile().exists()).isTrue();
    return newFile;
  }
}

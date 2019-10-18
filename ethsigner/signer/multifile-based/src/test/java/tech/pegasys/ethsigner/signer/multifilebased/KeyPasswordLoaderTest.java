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
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.ADDRESS_1;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.ADDRESS_2;
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
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(ADDRESS_1);

    final Optional<KeyPasswordFile> loadedKeyPassFile = loader.loadKeyAndPassword(ADDRESS_1);

    assertThat(loadedKeyPassFile).isNotEmpty();
    assertThat(loadedKeyPassFile.get()).isEqualTo(keyPasswordFile);
  }

  @Test
  void loadKeyPasswordWithMissingPasswordReturnsEmpty() throws IOException {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(ADDRESS_1);
    Files.delete(keyPasswordFile.getPassword());

    final Optional<KeyPasswordFile> loadedKeyPassFile = loader.loadKeyAndPassword(ADDRESS_1);

    assertThat(loadedKeyPassFile).isEmpty();
  }

  @Test
  void loadKeyPasswordWithMissingKeyReturnsEmpty() throws IOException {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(ADDRESS_1);
    Files.delete(keyPasswordFile.getKey());

    final Optional<KeyPasswordFile> loadedKeyPassFile = loader.loadKeyAndPassword(ADDRESS_1);

    assertThat(loadedKeyPassFile).isEmpty();
  }

  @Test
  void loadKeyPasswordWithMissingKeyAndPasswordReturnsEmpty() throws IOException {
    final KeyPasswordFile keyPasswordFile = copyKeyPasswordToKeysDirectory(ADDRESS_1);
    Files.delete(keyPasswordFile.getKey());
    Files.delete(keyPasswordFile.getPassword());

    final Optional<KeyPasswordFile> loadedKeyPassFile = loader.loadKeyAndPassword(ADDRESS_1);

    assertThat(loadedKeyPassFile).isEmpty();
  }

  @Test
  void loadAvailableKeysReturnsAllValidKeyPasswordFilesInKeysDirectory() throws IOException {
    final KeyPasswordFile kpFile1 = copyKeyPasswordToKeysDirectory(ADDRESS_1);
    final KeyPasswordFile kpFile2 = copyKeyPasswordToKeysDirectory(ADDRESS_2);

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).hasSize(2);
    assertThat(keyPasswordFiles).containsOnly(kpFile1, kpFile2);
  }

  @Test
  void loadAvailableKeysIgnoresKeyWithoutMatchingPassword() throws IOException {
    final KeyPasswordFile kpFile = copyKeyPasswordToKeysDirectory(ADDRESS_1);
    Files.delete(kpFile.getPassword());

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).isEmpty();
  }

  @Test
  void loadAvailableKeysIgnoresPasswordWithoutMatchingKey() throws IOException {
    final KeyPasswordFile kpFile = copyKeyPasswordToKeysDirectory(ADDRESS_1);
    Files.delete(kpFile.getKey());

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).isEmpty();
  }

  @Test
  void loadAvailableKeysIgnoresKeyFileWithNonLowercaseName() throws IOException {
    final KeyPasswordFile kpFile = copyKeyPasswordToKeysDirectory(ADDRESS_1);
    renameFile(kpFile.getKey(), ADDRESS_1.toUpperCase());

    final Collection<KeyPasswordFile> keyPasswordFiles = loader.loadAvailableKeys();

    assertThat(keyPasswordFiles).isEmpty();
  }

  private KeyPasswordFile copyKeyPasswordToKeysDirectory(final String address) {
    final KeyPasswordFile kpFile = loadKeyPasswordFile(address);
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

  private void renameFile(final Path file, final String newName) throws IOException {
    Files.move(file, file.getParent().resolve(newName));
  }
}
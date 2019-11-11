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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class KeyPasswordLoader {

  private static final Logger LOG = LogManager.getLogger();

  private static final String KEY_FILE_EXTENSION = ".key";
  private static final String PASSWORD_FILE_EXTENSION = ".password";
  private static final String GLOB_KEY_MATCHER = "**" + KEY_FILE_EXTENSION;

  private final Path keysDirectory;

  KeyPasswordLoader(final Path keysDirectory) {
    this.keysDirectory = keysDirectory;
  }

  Optional<KeyPasswordFile> loadKeyAndPasswordForAddress(final String address) {
    final List<KeyPasswordFile> matchingKeys =
        loadAvailableKeys().stream()
            .filter(kp -> kp.getFilename().toLowerCase().endsWith(normalizeAddress(address)))
            .collect(Collectors.toList());

    if (matchingKeys.size() > 1) {
      LOG.error("Found multiple key/password matches for address {}", address);
      return Optional.empty();
    } else if (matchingKeys.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingKeys.get(0));
    }
  }

  Collection<KeyPasswordFile> loadAvailableKeys() {
    final Collection<KeyPasswordFile> keysAndPasswords = new HashSet<>();

    try (final DirectoryStream<Path> directoryStream =
        Files.newDirectoryStream(keysDirectory, GLOB_KEY_MATCHER)) {
      for (final Path file : directoryStream) {
        tryFindingMatchingPassword(file)
            .ifPresent(
                passwordFile -> keysAndPasswords.add(new KeyPasswordFile(file, passwordFile)));
      }
      return keysAndPasswords;
    } catch (final IOException e) {
      LOG.warn("Error searching for key/password files", e);
      return Collections.emptySet();
    }
  }

  private Optional<Path> tryFindingMatchingPassword(final Path keyFile) {
    final String passwordFilePath =
        keyFile.toAbsolutePath().toString().replace(KEY_FILE_EXTENSION, PASSWORD_FILE_EXTENSION);
    final File file = Paths.get(passwordFilePath).toFile();
    if (file.exists()) {
      return Optional.of(file.toPath());
    } else {
      return Optional.empty();
    }
  }

  private String normalizeAddress(final String address) {
    if (address.startsWith("0x")) {
      return address.replace("0x", "").toLowerCase();
    } else {
      return address.toLowerCase();
    }
  }
}

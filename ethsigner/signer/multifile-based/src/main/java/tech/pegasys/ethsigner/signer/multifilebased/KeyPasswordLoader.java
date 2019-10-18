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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

class KeyPasswordLoader {

  private static final int VALID_ADDRESS_LENGTH = 40;
  private static final String KEY_FILE_EXTENSION = ".key";
  private static final String PASSWORD_FILE_EXTENSION = ".password";
  private final PathMatcher KEY_FILE_MATCHER =
      FileSystems.getDefault().getPathMatcher("glob:**" + KEY_FILE_EXTENSION);

  private final Path keysDirectory;

  KeyPasswordLoader(final Path keysDirectory) {
    this.keysDirectory = keysDirectory;
  }

  Optional<KeyPasswordFile> loadKeyAndPassword(final String keyFilename) {
    final Path keyPath = keysDirectory.resolve(keyFilename.toLowerCase() + KEY_FILE_EXTENSION);
    if (keyPath.toFile().exists() && isFilenameValid(keyPath)) {
      return tryFindingMatchingPassword(keyPath).map(path -> new KeyPasswordFile(keyPath, path));
    } else {
      return Optional.empty();
    }
  }

  Collection<KeyPasswordFile> loadAvailableKeys() throws IOException {
    final Collection<KeyPasswordFile> keysAndPasswords = new HashSet<>();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(keysDirectory)) {
      for (Path keyFile : stream) {
        if (KEY_FILE_MATCHER.matches(keyFile) && isFilenameValid(keyFile)) {
          tryFindingMatchingPassword(keyFile)
              .ifPresent(
                  passwordFile -> keysAndPasswords.add(new KeyPasswordFile(keyFile, passwordFile)));
        }
      }

      return keysAndPasswords;
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

  private boolean isFilenameValid(final Path file) {
    final String filename = file.getFileName().toString();
    if (illegalExtension(filename) || illegalLength(filename) || notLowercase(filename)) {
      return false;
    } else {
      return true;
    }
  }

  private boolean illegalExtension(final String filename) {
    return !(filename.endsWith(KEY_FILE_EXTENSION) || filename.endsWith(PASSWORD_FILE_EXTENSION));
  }

  private boolean illegalLength(final String filename) {
    return Iterables.get(Splitter.on('.').split(filename), 0).length() != VALID_ADDRESS_LENGTH;
  }

  private boolean notLowercase(final String filename) {
    return !filename.equals(filename.toLowerCase());
  }
}

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

class KeyPasswordLoader {

  private final PathMatcher KEY_FILE_MATCHER =
      FileSystems.getDefault().getPathMatcher("glob:**.key");

  private final Path keysDirectory;

  KeyPasswordLoader(final Path keysDirectory) {
    this.keysDirectory = keysDirectory;
  }

  Optional<KeyPasswordFile> loadKeyPassword(final Path keyPath) {
    final Path resolvedPath = keysDirectory.resolve(keyPath);
    if (resolvedPath.toFile().exists()) {
      final Optional<Path> possiblePasswordFile = tryFindingMatchingPassword(resolvedPath);
      return possiblePasswordFile.map(path -> new KeyPasswordFile(keyPath, path));
    } else {
      return Optional.empty();
    }
  }

  Collection<KeyPasswordFile> loadAvailableKeys() throws IOException {
    final Collection<KeyPasswordFile> keysAndPasswords = new HashSet<>();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(keysDirectory)) {
      for (Path keyFile : stream) {
        if (KEY_FILE_MATCHER.matches(keyFile)) {
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
        keyFile.toAbsolutePath().toString().replace(".key", ".password");
    final File file = Paths.get(passwordFilePath).toFile();
    if (file.exists()) {
      return Optional.of(file.toPath());
    } else {
      return Optional.empty();
    }
  }
}

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

import java.nio.file.Path;

import com.google.common.base.Objects;

class FileBasedSigningMetadataFile {

  private final Path keyPath;
  private final Path passwordPath;
  private final String filename;

  public FileBasedSigningMetadataFile(
      final String filename, final Path keyPath, final Path passwordPath) {
    this.filename = getFilenameWithoutExtension(filename);
    this.keyPath = keyPath;
    this.passwordPath = passwordPath;
  }

  String getFilename() {
    return filename;
  }

  Path getKeyPath() {
    return keyPath;
  }

  Path getPasswordPath() {
    return passwordPath;
  }

  private String getFilenameWithoutExtension(final String filename) {
    if (filename.endsWith(".toml")) {
      return filename.replaceAll("\\.toml", "");
    } else {
      throw new IllegalArgumentException("Invalid TOML config filename extension: " + filename);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final FileBasedSigningMetadataFile that = (FileBasedSigningMetadataFile) o;
    return Objects.equal(filename, that.filename)
        && Objects.equal(keyPath, that.keyPath)
        && Objects.equal(passwordPath, that.passwordPath);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(filename, keyPath, passwordPath);
  }
}

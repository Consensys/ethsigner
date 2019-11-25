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

  private final Path file;
  private final Path key;
  private final Path password;
  private final String filename;

  FileBasedSigningMetadataFile(final Path file, final Path key, final Path password) {
    this.file = file;
    this.key = key;
    this.password = password;
    this.filename = getFilenameWithoutExtension(file);
  }

  String getFilename() {
    return filename;
  }

  Path getKey() {
    return key;
  }

  Path getPassword() {
    return password;
  }

  private String getFilenameWithoutExtension(final Path file) {
    final String filename = file.getFileName().toString();
    if (filename.endsWith(".toml")) {
      return filename.replaceAll("\\.toml", "");
    } else {
      throw new IllegalArgumentException("Invalid TOML config filename extension");
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
    return Objects.equal(file, that.file)
        && Objects.equal(key, that.key)
        && Objects.equal(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(file, key, password);
  }
}

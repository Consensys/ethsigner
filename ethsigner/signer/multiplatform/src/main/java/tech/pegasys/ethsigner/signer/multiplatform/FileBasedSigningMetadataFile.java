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

import com.google.common.base.Objects;
import java.nio.file.Path;

class FileBasedSigningMetadataFile {

  private final Path file;
  private final Path keyFilename;
  private final Path passwordFilename;
  private final String type;


  FileBasedSigningMetadataFile(final Path file, final Path keyFilename, final Path passwordFilename) {
    this.type = "file-based"; // TODO make an enum
    this.file = file;
    this.keyFilename = keyFilename;
    this.passwordFilename = passwordFilename;
  }

  String getType() { return type; }
  String getFilename() { return getFilenameWithoutExtension(file); }

  Path getKeyFilename() { return keyFilename; }
  Path getPasswordFilename() { return passwordFilename; }

  private String getFilenameWithoutExtension(final Path file) {
    final String filename = file.getFileName().toString();
    if (filename.endsWith(".config")) {
      return filename.replaceAll("\\.config", "");
    } else {
      throw new IllegalArgumentException("Invalid config filename extension");
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
    return Objects.equal(keyFilename, that.keyFilename)
        && Objects.equal(passwordFilename, that.passwordFilename)
        && Objects.equal(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(keyFilename, passwordFilename, type);
  }
}

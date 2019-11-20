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

class MetadataFile {

  private final Path file;
  private final String type;
  private final String filename;

  MetadataFile(final Path file) {
    this.file = file;
    this.type = "file-based"; // TODO make an enum
    this.filename = file.getFileName().toString();
  }

  String getFilename() {
    return filename;
  }
  String getType() { return type; }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final MetadataFile that = (MetadataFile) o;
    return Objects.equal(filename, that.filename)
        && Objects.equal(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(filename, type);
  }
}

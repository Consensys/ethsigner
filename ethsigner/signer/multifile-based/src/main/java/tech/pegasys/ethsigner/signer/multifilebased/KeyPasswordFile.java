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

import java.nio.file.Path;

import com.google.common.base.Objects;

class KeyPasswordFile {

  private final String address;
  private final Path key;
  private final Path password;

  KeyPasswordFile(final Path key, final Path password) {
    this.key = key;
    this.password = password;

    if (!keyAndPasswordNameMatch(key, password)) {
      throw new IllegalArgumentException("Key and Password names must match");
    } else {
      this.address = key.toFile().getName().replace(".key", "");
    }
  }

  private boolean keyAndPasswordNameMatch(final Path key, final Path password) {
    return key.toFile()
        .getName()
        .replace(".key", "")
        .equals(password.toFile().getName().replace(".password", ""));
  }

  String getAddress() {
    return address;
  }

  Path getKey() {
    return key;
  }

  Path getPassword() {
    return password;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final KeyPasswordFile that = (KeyPasswordFile) o;
    return Objects.equal(address, that.address)
        && Objects.equal(key, that.key)
        && Objects.equal(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(address, key, password);
  }
}

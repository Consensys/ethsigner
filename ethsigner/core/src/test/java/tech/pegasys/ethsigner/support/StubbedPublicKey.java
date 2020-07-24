/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.support;

import tech.pegasys.signers.secp256k1.api.PublicKey;

import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;

public class StubbedPublicKey implements PublicKey {

  private final String keyHexString;

  public StubbedPublicKey(final String keyHexString) {
    this.keyHexString = keyHexString;
  }

  @Override
  public byte[] getValue() {
    return Bytes.fromHexString(keyHexString).toArrayUnsafe();
  }

  @Override
  public String toString() {
    return keyHexString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final StubbedPublicKey other = (StubbedPublicKey) o;
    return keyHexString.equals(other.keyHexString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyHexString);
  }
}

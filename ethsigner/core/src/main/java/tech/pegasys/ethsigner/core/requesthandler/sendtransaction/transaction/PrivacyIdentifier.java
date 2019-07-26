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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.web3j.utils.Numeric;

public class PrivacyIdentifier implements Comparable<byte[]> {

  private static final int IDENTIFIER_LENGTH = 32;

  private final byte[] identifier;

  private PrivacyIdentifier(byte[] identifier) {
    this.identifier = identifier;
  }

  public static PrivacyIdentifier fromBase64String(final String input) {
    final byte[] byteRepresentation = Base64.getDecoder().decode(input);
    return createPrivacyIdentifier(input, byteRepresentation);
  }

  public static PrivacyIdentifier fromHexString(final String input) {
    final byte[] byteRepresentation = Numeric.hexStringToByteArray(input);
    return createPrivacyIdentifier(input, byteRepresentation);
  }

  private static PrivacyIdentifier createPrivacyIdentifier(
      final String inputString, final byte[] inputBytes) {
    if (inputBytes.length != IDENTIFIER_LENGTH) {
      throw new IllegalArgumentException(
          String.format("Public key did not contain 32 bytes: %s", inputString));
    }
    return new PrivacyIdentifier(inputBytes);
  }

  public byte[] getRaw() {
    return identifier;
  }

  public String asIso8559String() {
    return new String(identifier, StandardCharsets.ISO_8859_1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrivacyIdentifier that = (PrivacyIdentifier) o;
    return Arrays.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(identifier);
  }

  @Override
  public int compareTo(byte[] o) {
    return Arrays.compare(identifier, o);
  }
}

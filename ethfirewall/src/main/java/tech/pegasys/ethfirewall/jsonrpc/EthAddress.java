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
package tech.pegasys.ethfirewall.jsonrpc;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.io.BaseEncoding;

public class EthAddress {

  private static final int ADDRESS_LENGTH = 20;
  private final byte[] bytes;

  protected EthAddress(final byte[] bytes) {
    this.bytes = bytes;
  }

  //TODO replace with simple String endsWith()

  /**
   * Parse an hexadecimal string representing an account address.
   *
   * @param str An hexadecimal string (with or without the leading '0x') representing a valid
   *     account address.
   * @return The parsed address: {@code null} if the provided string is {@code null}.
   * @throws IllegalArgumentException if the string is either not hexadecimal, or not the valid
   *     representation of an address.
   */
  @JsonCreator
  public static EthAddress fromHexString(final String str) {
    if (str == null) {
      throw new IllegalArgumentException("Require a non-null String");
    }

    return new EthAddress(fromRawHexString(str));
  }

  @Override
  public String toString() {
    return String.format("0x%s", BaseEncoding.base16().encode(bytes).toLowerCase());
  }

  // TODO pull out the prefix parsing - common parsing component
  private static byte[] fromRawHexString(final String str) {
    String hex = str;
    if (str.startsWith("0x")) {
      hex = str.substring(2);
    }

    int len = hex.length();
    int idxShift = 0;
    if (len % 2 != 0) {
      throw new IllegalArgumentException("Invalid odd-length hex binary representation " + str);
    }

    final int size = len / 2;
    final int destSize;

    destSize = ADDRESS_LENGTH;
    checkArgument(
        size <= destSize,
        "Hex value %s is too big: expected at most %s bytes but got %s",
        str,
        destSize,
        size);

    final byte[] out = new byte[destSize];

    final int destOffset = (destSize - size);
    for (int i = 0; i < len; i += 2) {
      final int h = hexToBin(hex.charAt(i));
      final int l = hexToBin(hex.charAt(i + 1));
      if (h == -1) {
        throw new IllegalArgumentException(
            String.format(
                "Illegal character '%c' found at index %d in hex binary representation %s",
                hex.charAt(i), i - idxShift, str));
      }
      if (l == -1) {
        throw new IllegalArgumentException(
            String.format(
                "Illegal character '%c' found at index %d in hex binary representation %s",
                hex.charAt(i + 1), i + 1 - idxShift, str));
      }

      out[destOffset + (i / 2)] = (byte) (h * 16 + l);
    }
    return out;
  }

  private static int hexToBin(final char ch) {
    if ('0' <= ch && ch <= '9') {
      return ch - 48;
    } else if ('A' <= ch && ch <= 'F') {
      return ch - 65 + 10;
    } else {
      return 'a' <= ch && ch <= 'f' ? ch - 97 + 10 : -1;
    }
  }
}

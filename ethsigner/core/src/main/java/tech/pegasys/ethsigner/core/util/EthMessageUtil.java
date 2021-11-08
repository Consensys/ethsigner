/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.ethsigner.core.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Locale;

import org.apache.tuweni.bytes.Bytes;

// adapted from
// https://github.com/web3j/web3j/blob/7054ab324cab0d44a759780e61ee12978fd17490/crypto/src/main/java/org/web3j/crypto/Sign.java#L59
public class EthMessageUtil {
  static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

  /**
   * Converts input message (hex or literal string) to Ethereum specific message so that it can be
   * signed. <code>\x19Ethereum Signed Message:\n" + len(message) + message</code>
   *
   * @param message Hex String (starts with 0x) or literal string
   * @return byte[] of ethereum formatted message ready to be signed
   */
  public static byte[] getEthereumMessage(final String message) {
    final byte[] messageBytes = hexToBytes(message);
    final byte[] prefix = getEthereumMessagePrefix(messageBytes.length);

    byte[] result = new byte[prefix.length + messageBytes.length];
    System.arraycopy(prefix, 0, result, 0, prefix.length);
    System.arraycopy(messageBytes, 0, result, prefix.length, messageBytes.length);

    return result;
  }

  static byte[] getEthereumMessagePrefix(int messageLength) {
    return MESSAGE_PREFIX.concat(String.valueOf(messageLength)).getBytes(UTF_8);
  }

  static byte[] hexToBytes(final String message) {
    if (message.toLowerCase(Locale.US).startsWith("0x")) {
      return Bytes.fromHexString(message).toArray();
    }
    return message.getBytes(UTF_8);
  }
}

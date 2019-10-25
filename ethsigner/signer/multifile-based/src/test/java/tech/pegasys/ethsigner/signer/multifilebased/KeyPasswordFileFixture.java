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

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

/*
 The core of the key/password loading relies on FileBasedSignerFactory.createSigner() that is a
 static method. This makes mocking really hard.

 To avoid any black magic, we have a set of valid key/password files that are used on the unit
 tests.
*/
class KeyPasswordFileFixture {

  public static String NO_PREFIX_LOWERCASE_KP_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef57";
  public static String NO_PREFIX_LOWERCASE_KP = "627306090abab3a6e1400e9345bc60c78a8bef57";

  public static String PREFIX_MIXEDCASE_KP_ADDRESS = "f17f52151ebef6c7334fad080c5704d77216b732";
  public static String PREFIX_MIXEDCASE_KP =
      "UTC--2019-10-23T04-00-04.860366000Z--f17f52151ebef6c7334fad080c5704d77216b732";

  public static String PREFIX_LOWERCASE_KP_ADDRESS = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";
  public static String PREFIX_LOWERCASE_KP = "1571803966_fe3b557e8fb62b89f4916b721be55ceb828dbd73";

  private static final Path keysDirectory = Path.of("src/test/resources/keys");

  static KeyPasswordFile loadKeyPasswordFile(final String filename) {
    final Path keyPath = keysDirectory.resolve(filename + ".key");
    if (!keyPath.toFile().exists()) {
      fail("Missing key file " + keyPath.getFileName().toString());
      return null;
    }

    final Path passwordPath = keysDirectory.resolve(filename + ".password");
    if (!passwordPath.toFile().exists()) {
      fail("Missing password file " + passwordPath.getFileName().toString());
      return null;
    }

    return new KeyPasswordFile(keyPath, passwordPath);
  }
}

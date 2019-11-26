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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Path;

public class MetadataFileFixture {

  public static final String CONFIG_FILE_EXTENSION = ".toml";
  public static String NO_PREFIX_LOWERCASE_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef57";
  public static String PREFIX_ADDRESS = "bar_fe3b557e8fb62b89f4916b721be55ceb828dbd73";
  public static String PREFIX_ADDRESS_UNKNOWN_TYPE_SIGNER =
      "bar_fe3b557e8fb62b89f4916b721be55ceb828dbd75";
  public static String MISSING_KEY_PATH_ADDRESS = "fe3b557e8fb62b89f4916b721be55ceb828dbd76";
  public static String MISSING_PASSWORD_PATH_ADDRESS = "fe3b557e8fb62b89f4916b721be55ceb828dbd77";
  public static String MISSING_KEY_AND_PASSWORD_PATH_ADDRESS =
      "fe3b557e8fb62b89f4916b721be55ceb828dbd78";
  public static String PREFIX_MIXEDCASE_KP =
      "UTC--2019-10-23T04-00-04.860366000Z--f17f52151ebef6c7334fad080c5704d77216b732";

  public static String KEY_FILE = "src/test/resources/metadata-toml-configs/k.key";
  public static String PASSWORD_FILE = "src/test/resources/metadata-toml-configs/p.password";

  private static final Path metadataTomlConfigsDirectory =
      Path.of("src/test/resources/metadata-toml-configs");

  static FileBasedSigningMetadataFile load(
      final String metadataFilename, final String keyFilename, final String passwordFilename) {
    final Path metadataPath =
        metadataTomlConfigsDirectory.resolve(metadataFilename + CONFIG_FILE_EXTENSION);
    if (!metadataPath.toFile().exists()) {
      fail("Missing metadata TOML file " + metadataPath.getFileName().toString());
      return null;
    }

    return new FileBasedSigningMetadataFile(
        metadataPath.getFileName().toString(),
        new File(keyFilename).toPath(),
        new File(passwordFilename).toPath());
  }
}

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
package tech.pegasys.ethsigner.tests.multiplatformsigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AzureSignerMultiplatformSigning {

  private static final String clientId = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
  private static final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");

  private final String AZURE_ETHEREUM_ADDRESS = "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73";
  private final String FILE_ETHEREUM_ADDRESS = "0xa01f618424b0113a9cebdc6cb66ca5b48e9120c5";
  private Signer ethSigner;

  @TempDir Path tomlDirectory;

  @BeforeAll
  public static void preChecks() {
    Assumptions.assumeTrue(
        clientId != null && clientSecret != null,
        "Ensure Azure client id and client secret env variables are set");
  }

  @AfterEach
  public void cleanUp() {
    if (ethSigner != null) {
      ethSigner.shutdown();
      ethSigner = null;
    }
  }

  public void setup() {
    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withMultiplatformSignerDirectory(tomlDirectory).build();
    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();

    ethSigner = new Signer(signerConfig, nodeConfig, new NodePorts(1, 2));
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  private void createAzureTomlFileAt(final String tomlFilename) {
    try {
      final Path tomlFilePath = tomlDirectory.resolve(tomlFilename);

      final FileWriter writer = new FileWriter(tomlFilePath.toFile(), StandardCharsets.UTF_8);
      writer.write("[signing]\n");
      writer
          .append("type = \"azure-based-signer\"\n")
          .append("key-vault-name = \"ethsignertestkey\"\n")
          .append("key-name = \"TestKey\"\n")
          .append("key-version = \"7c01fe58d68148bba5824ce418241092\"\n")
          .append("client-id = \"" + clientId + "\"\n")
          .append("client-secret = \"" + clientSecret + "\"\n");
      writer.close();
    } catch (final IOException e) {
      fail("Unable to create Azure TOML file.");
    }
  }

  private void createFileBasedTomlFileAt(
      final String tomlFilename, final String keyPath, final String passwordPath) {
    try {
      final Path tomlFilePath = tomlDirectory.resolve(tomlFilename);

      final FileWriter writer = new FileWriter(tomlFilePath.toFile(), StandardCharsets.UTF_8);
      writer.write("[signing]\n");
      writer
          .append("type = \"file-based-signer\"\n")
          .append("key-file = \"" + keyPath + "\"\n")
          .append("password-file = \"" + passwordPath + "\"\n");
      writer.close();
    } catch (final IOException e) {
      fail("Unable to create Azure TOML file.");
    }
  }

  @Test
  void multipleSignersAreCreatedAndExpectedAddressAreReported() throws URISyntaxException {

    createAzureTomlFileAt(AZURE_ETHEREUM_ADDRESS + ".toml");
    createFileBasedTomlFileAt(
        "a01f618424b0113a9cebdc6cb66ca5b48e9120c5.toml",
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath());

    setup();

    assertThat(ethSigner.accounts().list())
        .containsOnly(AZURE_ETHEREUM_ADDRESS, FILE_ETHEREUM_ADDRESS);
  }

  @Test
  void incorrectlyNamedAzureFileIsNotLoaded() {
    createAzureTomlFileAt("ffffffffffffffffffffffffffffffffffffffff.toml");

    setup();

    assertThat(ethSigner.accounts().list()).isEmpty();
  }

  @Test
  void incorrectlyNamedFileBasedSignerIsNotLoaded() throws URISyntaxException {
    createFileBasedTomlFileAt(
        "ffffffffffffffffffffffffffffffffffffffff.toml",
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath());
    setup();

    assertThat(ethSigner.accounts().list()).isEmpty();
  }
}

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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AzureSignerMultiplatformSigning {

  private static final String clientId = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
  private static final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");

  final String AZURE_ETHEREUM_ADDRESS = "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73";

  private static Signer ethSigner;

  @TempDir static Path tomlDirectory;

  @BeforeAll
  public static void preChecks() {
    Assumptions.assumeTrue(
        clientId != null && clientSecret != null,
        "Ensure Azure client id and client secret env variables are set");
  }

  public void setup() {
    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withMetaDataLibrary(tomlDirectory).build();
    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();

    ethSigner = new Signer(signerConfig, nodeConfig, new NodePorts(1, 2));
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  private void createAzureTomlFileAt(final Path tomlFile) {
    try {
      final FileWriter writer = new FileWriter(tomlFile.toFile(), StandardCharsets.UTF_8);
      writer.write("[signing]\n");
      writer.append("type = \"azure-based-signer\"\n");
      writer.append("key-vault-name = \"ethsignertestkey\"\n");
      writer.append("key-name = \"TestKey\"\n");
      writer.append("key-version = \"7c01fe58d68148bba5824ce418241092\"\n");
      writer.append("client-id = \"" + clientId + "\"\n");
      writer.append("client-secret = \"" + clientSecret + "\"\n");
      writer.close();
    } catch (final IOException e) {
      fail("Unable to create azure toml file.");
    }
  }

  @Test
  public void azureSignerIsCreatedAndReportsAddress() {
    final Path azureConfigFile = tomlDirectory.resolve(AZURE_ETHEREUM_ADDRESS + ".toml");
    createAzureTomlFileAt(azureConfigFile);

    setup();

    assertThat(ethSigner.accounts().list()).containsOnly(AZURE_ETHEREUM_ADDRESS);
  }

  @Test
  public void incorrectlyNamedAzureFileIsNotLoaded() {
    final Path azureConfigFile = tomlDirectory.resolve("invalidAddress.toml");
    createAzureTomlFileAt(azureConfigFile);

    setup();

    assertThat(ethSigner.accounts().list()).isEmpty();
  }
}

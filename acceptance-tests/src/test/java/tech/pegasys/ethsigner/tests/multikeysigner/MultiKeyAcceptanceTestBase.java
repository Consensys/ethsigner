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
package tech.pegasys.ethsigner.tests.multikeysigner;

import static org.assertj.core.api.AssertionsForClassTypes.fail;

import tech.pegasys.ethsigner.tests.dsl.hashicorp.HashicorpNode;
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
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;

public class MultiKeyAcceptanceTestBase {

  protected Signer ethSigner;

  @AfterEach
  public void cleanUp() {
    if (ethSigner != null) {
      ethSigner.shutdown();
      ethSigner = null;
    }
  }

  void setup(final Path tomlDirectory) {
    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withMultiKeySignerDirectory(tomlDirectory).build();
    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();

    ethSigner = new Signer(signerConfig, nodeConfig, new NodePorts(1, 2));
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  public void createAzureTomlFileAt(
      final Path tomlPath, final String clientId, final String clientSecret) {
    StringBuilder builder = new StringBuilder("[signing]\n");
    builder.append(tomlString.apply("type", "azure-signer"));
    builder.append(tomlString.apply("key-vault-name", "ethsignertestkey"));
    builder.append(tomlString.apply("key-name", "TesKey"));
    builder.append(tomlString.apply("key-version", "7c01fe58d68148bba5824ce418241092"));
    builder.append(tomlString.apply("client-id", clientId));
    builder.append(tomlString.apply("client-secret", clientSecret));
    createTomlFile(tomlPath, builder.toString());
  }

  public void createFileBasedTomlFileAt(
      final Path tomlPath, final String keyPath, final String passwordPath) {
    StringBuilder builder = new StringBuilder("[signing]\n");
    builder.append(tomlString.apply("type", "file-based-signer"));
    builder.append(tomlString.apply("key-file", keyPath));
    builder.append(tomlString.apply("password-file", passwordPath));

    createTomlFile(tomlPath, builder.toString());
  }

  public void createHashicorpTomlFileAt(
      final Path tomlPath, final String authFile, final HashicorpNode hashicorpNode) {

    final StringBuilder builder = new StringBuilder();
    builder
        .append("[signing]\n")
        .append(tomlString.apply("type", "hashicorp-signer"))
        .append(tomlString.apply("signing-key-path", hashicorpNode.getSigningKeyPath()))
        .append(tomlString.apply("host", hashicorpNode.getHost()))
        .append(tomlInteger.apply("port", hashicorpNode.getPort()))
        .append(tomlString.apply("auth-file", authFile))
        .append(tomlInteger.apply("timeout", 500))
        .append(tomlBoolean.apply("tls-enabled", hashicorpNode.isTlsEnabled()));
    hashicorpNode
        .getKnownServerFilePath()
        .ifPresent(
            config -> builder.append(tomlString.apply("tls-known-server-file", config.toString())));
    final String toml = builder.toString();

    createTomlFile(tomlPath, toml);
  }

  private void createTomlFile(final Path tomlPath, final String toml) {
    try (final FileWriter fileWriter = new FileWriter(tomlPath.toFile(), StandardCharsets.UTF_8)) {
      fileWriter.write(toml);
    } catch (final IOException e) {
      fail("Unable to create TOML file.");
    }
  }

  private BiFunction<String, String, String> tomlString =
      (key, value) -> String.format("%s=\"%s\"\n", key, value);
  private BiFunction<String, Integer, String> tomlInteger =
      (key, value) -> String.format("%s=%d\n", key, value);
  private BiFunction<String, Boolean, String> tomlBoolean =
      (key, value) -> String.format("%s=%b\n", key, value);
}

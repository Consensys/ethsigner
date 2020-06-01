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
import static tech.pegasys.signers.hashicorp.dsl.certificates.CertificateHelpers.createFingerprintFile;

import tech.pegasys.ethsigner.tests.dsl.node.HashicorpSigningParams;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.signers.hashicorp.dsl.certificates.SelfSignedCertificate;
import tech.pegasys.signers.hashicorp.util.HashicorpConfigUtil;
import tech.pegasys.signers.secp256k1.common.TomlStringBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "azure-signer")
            .withQuotedString("key-vault-name", "ethsignertestkey")
            .withQuotedString("key-name", "TestKey")
            .withQuotedString("key-version", "7c01fe58d68148bba5824ce418241092")
            .withQuotedString("client-id", clientId)
            .withQuotedString("client-secret", clientSecret)
            .build();
    createTomlFile(tomlPath, toml);
  }

  public void createFileBasedTomlFileAt(
      final Path tomlPath, final String keyPath, final String passwordPath) {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "file-based-signer")
            .withQuotedString("key-file", keyPath)
            .withQuotedString("password-file", passwordPath)
            .build();

    createTomlFile(tomlPath, toml);
  }

  public void createHashicorpTomlFileAt(
      final Path tomlPath, final HashicorpSigningParams hashicorpNode) {

    try {
      final Optional<SelfSignedCertificate> tlsCert = hashicorpNode.getServerCertificate();
      String trustStorePath = null;
      if (tlsCert.isPresent()) {
        trustStorePath =
            createFingerprintFile(
                    tomlPath.getParent(), tlsCert.get(), Optional.of(hashicorpNode.getPort()))
                .toString();
      }

      final String hashicorpSignerToml =
          HashicorpConfigUtil.createTomlConfig(
              hashicorpNode.getHost(),
              hashicorpNode.getPort(),
              hashicorpNode.getVaultToken(),
              hashicorpNode.getSecretHttpPath(),
              hashicorpNode.getSecretName(),
              10_000,
              tlsCert.isPresent(),
              tlsCert.map(ignored -> "WHITELIST").orElse(null),
              trustStorePath,
              null);

      final TomlStringBuilder tomlBuilder = new TomlStringBuilder("signing");
      tomlBuilder.withQuotedString("type", "hashicorp-signer");
      final String toml = tomlBuilder.build() + hashicorpSignerToml;

      createTomlFile(tomlPath, toml);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to construct a valid hashicorp TOML file", e);
    }
  }

  private void createTomlFile(final Path tomlPath, final String toml) {
    try {
      Files.writeString(tomlPath, toml);
    } catch (final IOException e) {
      fail("Unable to create TOML file.");
    }
  }
}

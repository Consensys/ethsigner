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
package tech.pegasys.ethsigner.signer.multikey;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.signer.multikey.metadata.HashicorpSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.SigningMetadataFile;
import tech.pegasys.ethsigner.toml.util.TomlStringBuilder;
import tech.pegasys.signers.hashicorp.config.ConnectionParameters;
import tech.pegasys.signers.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.signers.hashicorp.util.HashicorpConfigUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HashicorpSigningMetadataTomlConfigLoaderTest {
  @TempDir Path configsDirectory;

  private SigningMetadataTomlConfigLoader loader;

  @BeforeEach
  void beforeEach() {
    loader = new SigningMetadataTomlConfigLoader(configsDirectory);
  }

  @Test
  void hashicorpConfigIsLoadedIfHashicorpMetadataFileInDirectory() throws IOException {
    final String hashicorpSignerToml =
        HashicorpConfigUtil.createTomlConfig(
            "Host", 9999, "token", "/path/to/key", "key_name", 10000, true, null, null, null);

    final TomlStringBuilder tomlBuilder = new TomlStringBuilder("signing");
    tomlBuilder.withQuotedString("type", "hashicorp-signer");

    final String toml = tomlBuilder.build() + hashicorpSignerToml;
    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(HashicorpSigningMetadataFile.class);
    final HashicorpSigningMetadataFile metadataFile =
        (HashicorpSigningMetadataFile) metadataFiles.toArray()[0];

    final HashicorpKeyConfig hashicorpConfig = metadataFile.getConfig();

    final ConnectionParameters connectionParams = hashicorpConfig.getConnectionParams();
    assertThat(connectionParams.getServerHost()).isEqualTo("Host");
    assertThat(connectionParams.getServerPort().get()).isEqualTo(9999);
    assertThat(connectionParams.getTimeoutMilliseconds().get()).isEqualTo(10000);
    assertThat(connectionParams.getTlsOptions()).isNotEmpty();
    assertThat(connectionParams.getTlsOptions().get().getTrustStoreType()).isEmpty();
    assertThat(hashicorpConfig.getKeyDefinition().getKeyPath()).isEqualTo("/path/to/key");
    assertThat(hashicorpConfig.getKeyDefinition().getKeyName().get()).isEqualTo("key_name");
    assertThat(hashicorpConfig.getKeyDefinition().getToken()).isEqualTo("token");
  }

  private void createTomlFile(final String toml) throws IOException {
    // creates metadata
    final String metaDataToml =
        new TomlStringBuilder("metadata")
            .withNonQuotedString("createdAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            .withQuotedString("description", "Test Multisign Toml")
            .build();
    Files.write(
        Files.createTempFile(configsDirectory, "test", ".toml"), List.of(metaDataToml, toml));
  }
}

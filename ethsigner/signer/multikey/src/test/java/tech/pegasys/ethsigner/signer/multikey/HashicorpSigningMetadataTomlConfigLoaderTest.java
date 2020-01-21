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

import tech.pegasys.ethsigner.signer.hashicorp.HashicorpConfig;
import tech.pegasys.ethsigner.signer.multikey.metadata.HashicorpSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.SigningMetadataFile;
import tech.pegasys.ethsigner.toml.util.TomlStringBuilder;

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
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "hashicorp-signer")
            .withQuotedString("signing-key-path", "/path/to/key")
            .withQuotedString("host", "Host")
            .withNonQuotedString("port", "9999")
            .withQuotedString("auth-file", "/path/to/auth-file")
            .withNonQuotedString("timeout", "50")
            .build();

    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(HashicorpSigningMetadataFile.class);
    final HashicorpSigningMetadataFile metadataFile =
        (HashicorpSigningMetadataFile) metadataFiles.toArray()[0];

    final HashicorpConfig hashicorpConfig = metadataFile.getConfig();
    assertThat(hashicorpConfig.getSigningKeyPath()).isEqualTo("/path/to/key");
    assertThat(hashicorpConfig.getHost()).isEqualTo("Host");
    assertThat(hashicorpConfig.getPort()).isEqualTo(9999);
    assertThat(hashicorpConfig.getAuthFilePath().toString()).isEqualTo("/path/to/auth-file");
    assertThat(hashicorpConfig.getTimeout()).isEqualTo(50);
    assertThat(hashicorpConfig.isTlsEnabled()).isTrue();
    assertThat(hashicorpConfig.getTlsKnownServerFile().isEmpty()).isTrue();
  }

  @Test
  void hashicorpConfigIsLoadedWithTlsDisabled() throws IOException {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "hashicorp-signer")
            .withQuotedString("signing-key-path", "/path/to/key")
            .withQuotedString("host", "Host")
            .withNonQuotedString("port", "9999")
            .withQuotedString("auth-file", "/path/to/auth-file")
            .withNonQuotedString("timeout", "50")
            .withNonQuotedString("tls-enabled", "false")
            .build();

    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(HashicorpSigningMetadataFile.class);
    final HashicorpSigningMetadataFile metadataFile =
        (HashicorpSigningMetadataFile) metadataFiles.toArray()[0];

    final HashicorpConfig hashicorpConfig = metadataFile.getConfig();
    assertThat(hashicorpConfig.getSigningKeyPath()).isEqualTo("/path/to/key");
    assertThat(hashicorpConfig.getHost()).isEqualTo("Host");
    assertThat(hashicorpConfig.getPort()).isEqualTo(9999);
    assertThat(hashicorpConfig.getAuthFilePath().toString()).isEqualTo("/path/to/auth-file");
    assertThat(hashicorpConfig.getTimeout()).isEqualTo(50);
    assertThat(hashicorpConfig.isTlsEnabled()).isFalse();
    assertThat(hashicorpConfig.getTlsKnownServerFile().isPresent()).isFalse();
  }

  @Test
  void hashicorpConfigIsLoadedWithTlsDisabledAndKnownServerFileSpecified() throws IOException {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "hashicorp-signer")
            .withQuotedString("signing-key-path", "/path/to/key")
            .withQuotedString("host", "Host")
            .withNonQuotedString("port", "9999")
            .withQuotedString("auth-file", "/path/to/auth-file")
            .withNonQuotedString("timeout", "50")
            .withNonQuotedString("tls-enabled", "false")
            .withQuotedString("tls-known-server-file", "/path/to/known-server-file")
            .build();

    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(HashicorpSigningMetadataFile.class);
    final HashicorpSigningMetadataFile metadataFile =
        (HashicorpSigningMetadataFile) metadataFiles.toArray()[0];

    final HashicorpConfig hashicorpConfig = metadataFile.getConfig();
    assertThat(hashicorpConfig.getSigningKeyPath()).isEqualTo("/path/to/key");
    assertThat(hashicorpConfig.getHost()).isEqualTo("Host");
    assertThat(hashicorpConfig.getPort()).isEqualTo(9999);
    assertThat(hashicorpConfig.getAuthFilePath().toString()).isEqualTo("/path/to/auth-file");
    assertThat(hashicorpConfig.getTimeout()).isEqualTo(50);
    assertThat(hashicorpConfig.isTlsEnabled()).isFalse();
    assertThat(hashicorpConfig.getTlsKnownServerFile().isPresent()).isFalse();
  }

  @Test
  void hashicorpConfigIsLoadedWithTlsAndKnownServerFileSpecified() throws IOException {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "hashicorp-signer")
            .withQuotedString("signing-key-path", "/path/to/key")
            .withQuotedString("host", "Host")
            .withNonQuotedString("port", "9999")
            .withQuotedString("auth-file", "/path/to/auth-file")
            .withNonQuotedString("timeout", "50")
            .withNonQuotedString("tls-enabled", "true")
            .withQuotedString("tls-known-server-file", "/path/to/known-server-file")
            .build();

    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(HashicorpSigningMetadataFile.class);
    final HashicorpSigningMetadataFile metadataFile =
        (HashicorpSigningMetadataFile) metadataFiles.toArray()[0];

    final HashicorpConfig hashicorpConfig = metadataFile.getConfig();
    assertThat(hashicorpConfig.getSigningKeyPath()).isEqualTo("/path/to/key");
    assertThat(hashicorpConfig.getHost()).isEqualTo("Host");
    assertThat(hashicorpConfig.getPort()).isEqualTo(9999);
    assertThat(hashicorpConfig.getAuthFilePath().toString()).isEqualTo("/path/to/auth-file");
    assertThat(hashicorpConfig.getTimeout()).isEqualTo(50);
    assertThat(hashicorpConfig.isTlsEnabled()).isTrue();
    assertThat(hashicorpConfig.getTlsKnownServerFile().isPresent()).isTrue();
  }

  @Test
  void hashicorpConfigWithIllegalValueTypeFailsToLoad() throws IOException {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "hashicorp-signer")
            .withQuotedString("signing-key-path", "/path/to/key")
            .withQuotedString("host", "Host")
            .withNonQuotedString("port", "9999")
            .withQuotedString("auth-file", "/path/to/auth-file")
            .withQuotedString("timeout", "timeout string")
            .build();

    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isZero();
  }

  @Test
  void hashicorpConfigWithMissingFieldFailsToLoad() throws IOException {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "hashicorp-signer")
            .withQuotedString("signing-key-path", "/path/to/key")
            .withQuotedString("host", "Host")
            .build();

    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isZero();
  }

  @Test
  void relativeHashicorpAuthFileIsRelativeToLibraryRoot() throws IOException {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "hashicorp-signer")
            .withQuotedString("signing-key-path", "/path/to/key")
            .withQuotedString("host", "Host")
            .withNonQuotedString("port", "9999")
            .withQuotedString("auth-file", "./path/to/auth-file")
            .withNonQuotedString("timeout", "50")
            .withNonQuotedString("tls-enabled", "true")
            .withQuotedString("tls-known-server-file", "./path/to/known-server-file")
            .build();

    createTomlFile(toml);

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs();

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(HashicorpSigningMetadataFile.class);
    final HashicorpSigningMetadataFile metadataFile =
        (HashicorpSigningMetadataFile) metadataFiles.toArray()[0];

    assertThat(metadataFile.getConfig().getAuthFilePath())
        .isEqualTo(configsDirectory.resolve("./path/to/auth-file"));
    assertThat(metadataFile.getConfig().getTlsKnownServerFile().get())
        .isEqualTo(configsDirectory.resolve("./path/to/known-server-file"));
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

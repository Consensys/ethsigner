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
package tech.pegasys.ethsigner.signer.multikey;

import tech.pegasys.ethsigner.signer.azure.AzureConfig.AzureConfigBuilder;
import tech.pegasys.ethsigner.signer.multikey.metadata.AzureSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.HashicorpSigningMetadataFile;
import tech.pegasys.ethsigner.signer.multikey.metadata.SigningMetadataFile;
import tech.pegasys.signers.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.signers.hashicorp.config.loader.toml.TomlConfigLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.toml.TomlInvalidTypeException;
import org.apache.tuweni.toml.TomlParseResult;
import org.apache.tuweni.toml.TomlTable;

class SigningMetadataTomlConfigLoader {

  private static final Logger LOG = LogManager.getLogger();

  private static final String CONFIG_FILE_EXTENSION = ".toml";
  private static final String GLOB_CONFIG_MATCHER = "**" + CONFIG_FILE_EXTENSION;

  private final Path tomlConfigsDirectory;

  SigningMetadataTomlConfigLoader(final Path rootDirectory) {
    this.tomlConfigsDirectory = rootDirectory;
  }

  Optional<SigningMetadataFile> loadMetadataForAddress(final String address) {
    final List<SigningMetadataFile> matchingMetadata =
        loadAvailableSigningMetadataTomlConfigs().stream()
            .filter(
                toml -> toml.getBaseFilename().toLowerCase().endsWith(normalizeAddress(address)))
            .collect(Collectors.toList());

    if (matchingMetadata.size() > 1) {
      LOG.error("Found multiple signing metadata TOML file matches for address " + address);
      return Optional.empty();
    } else if (matchingMetadata.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingMetadata.get(0));
    }
  }

  Collection<SigningMetadataFile> loadAvailableSigningMetadataTomlConfigs() {
    final Collection<SigningMetadataFile> metadataConfigs = new HashSet<>();

    try (final DirectoryStream<Path> directoryStream =
        Files.newDirectoryStream(tomlConfigsDirectory, GLOB_CONFIG_MATCHER)) {
      for (final Path file : directoryStream) {
        getMetadataInfo(file).ifPresent(metadataConfigs::add);
      }
      return metadataConfigs;
    } catch (final IOException e) {
      LOG.warn("Error searching for signing metadata TOML files", e);
      return Collections.emptySet();
    }
  }

  private Optional<SigningMetadataFile> getMetadataInfo(final Path file) {
    final String filename = file.getFileName().toString();

    try {
      final TomlParseResult result =
          TomlConfigFileParser.loadConfigurationFromFile(file.toAbsolutePath().toString());

      final Optional<TomlTableAdapter> signingTable =
          getSigningTableFrom(file.getFileName().toString(), result);
      if (signingTable.isEmpty()) {
        return Optional.empty();
      }

      final String type = signingTable.get().getString("type");
      if (SignerType.fromString(type).equals(SignerType.FILE_BASED_SIGNER)) {
        return getFileBasedSigningMetadataFromToml(filename, result);
      } else if (SignerType.fromString(type).equals(SignerType.AZURE_SIGNER)) {
        return getAzureBasedSigningMetadataFromToml(file.getFileName().toString(), result);
      } else if (SignerType.fromString(type).equals(SignerType.HASHICORP_SIGNER)) {
        return getHashicorpMetadataFromToml(file, result);
      } else {
        LOG.error("Unknown signing type in metadata: " + type);
        return Optional.empty();
      }
    } catch (final IllegalArgumentException | TomlInvalidTypeException e) {
      final String errorMsg = String.format("%s failed to decode: %s", filename, e.getMessage());
      LOG.error(errorMsg);
      return Optional.empty();
    } catch (final Exception e) {
      LOG.error("Could not load TOML file " + file, e);
      return Optional.empty();
    }
  }

  private Optional<SigningMetadataFile> getFileBasedSigningMetadataFromToml(
      final String filename, final TomlParseResult result) {
    final Optional<TomlTableAdapter> signingTable = getSigningTableFrom(filename, result);
    if (signingTable.isEmpty()) {
      return Optional.empty();
    }
    final TomlTableAdapter table = signingTable.get();

    final String keyFilename = table.getString("key-file");
    final Path keyPath = makeRelativePathAbsolute(keyFilename);
    final String passwordFilename = table.getString("password-file");
    final Path passwordPath = makeRelativePathAbsolute(passwordFilename);
    return Optional.of(new FileBasedSigningMetadataFile(filename, keyPath, passwordPath));
  }

  private Optional<SigningMetadataFile> getAzureBasedSigningMetadataFromToml(
      final String filename, final TomlParseResult result) {

    final Optional<TomlTableAdapter> signingTable = getSigningTableFrom(filename, result);
    if (signingTable.isEmpty()) {
      return Optional.empty();
    }

    final AzureConfigBuilder builder;
    final TomlTableAdapter table = signingTable.get();
    builder = new AzureConfigBuilder();
    builder.withKeyVaultName(table.getString("key-vault-name"));
    builder.withKeyName(table.getString("key-name"));
    builder.withKeyVersion(table.getString("key-version"));
    builder.withClientId(table.getString("client-id"));
    builder.withClientSecret(table.getString("client-secret"));
    return Optional.of(new AzureSigningMetadataFile(filename, builder.build()));
  }

  private Optional<SigningMetadataFile> getHashicorpMetadataFromToml(
      final Path inputFile, final TomlParseResult result) {

    final String filename = inputFile.getFileName().toString();

    final Optional<TomlTableAdapter> signingTable = getSigningTableFrom(filename, result);
    if (signingTable.isEmpty()) {
      return Optional.empty();
    }

    final HashicorpKeyConfig config = TomlConfigLoader.fromToml(inputFile, "signing");

    return Optional.of(new HashicorpSigningMetadataFile(filename, config));
  }

  private Optional<TomlTableAdapter> getSigningTableFrom(
      final String filename, final TomlParseResult result) {
    final TomlTable signingTable = result.getTable("signing");
    if (signingTable == null) {
      LOG.error(
          filename
              + " is a badly formed EthSigner metadata file - \"signing\" heading is missing.");
      return Optional.empty();
    }
    return Optional.of(new TomlTableAdapter(signingTable));
  }

  private String normalizeAddress(final String address) {
    if (address.startsWith("0x")) {
      return address.replace("0x", "").toLowerCase();
    } else {
      return address.toLowerCase();
    }
  }

  private Path makeRelativePathAbsolute(final String input) {
    final Path parsedInput = Path.of(input);

    if (parsedInput.isAbsolute()) {
      return parsedInput;
    }

    return tomlConfigsDirectory.resolve(input);
  }
}

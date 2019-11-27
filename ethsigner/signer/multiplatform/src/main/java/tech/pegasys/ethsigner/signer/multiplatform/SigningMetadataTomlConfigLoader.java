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

import java.io.File;
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
import org.apache.tuweni.toml.TomlParseResult;

class SigningMetadataTomlConfigLoader {

  private static final Logger LOG = LogManager.getLogger();

  private static final String CONFIG_FILE_EXTENSION = ".toml";
  private static final String GLOB_CONFIG_MATCHER = "**" + CONFIG_FILE_EXTENSION;

  private final Path tomlConfigsDirectory;

  SigningMetadataTomlConfigLoader(final Path rootDirectory) {
    this.tomlConfigsDirectory = rootDirectory;
  }

  Optional<FileBasedSigningMetadataFile> loadMetadataForAddress(final String address) {
    final List<FileBasedSigningMetadataFile> matchingMetadata =
        loadAvailableSigningMetadataTomlConfigs().stream()
            .filter(toml -> toml.getFilename().toLowerCase().endsWith(normalizeAddress(address)))
            .collect(Collectors.toList());

    if (matchingMetadata.size() > 1) {
      LOG.error("Found multiple signing metadata TOML file matches for address {}", address);
      return Optional.empty();
    } else if (matchingMetadata.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingMetadata.get(0));
    }
  }

  Collection<FileBasedSigningMetadataFile> loadAvailableSigningMetadataTomlConfigs() {
    final Collection<FileBasedSigningMetadataFile> metadataConfigs = new HashSet<>();

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

  private Optional<FileBasedSigningMetadataFile> getMetadataInfo(final Path file) {
    try {
      final TomlParseResult result =
          TomlConfigFileParser.loadConfigurationFromFile(file.toAbsolutePath().toString());
      final String type = result.getTable("signing").getString("type");
      if (SignerType.FILE_BASED_SIGNER.getType().equals(type)) {
        return getFileBasedSigningMetadataFromToml(file.getFileName().toString(), result);
      } else {
        LOG.error("Unknown signing type in metadata: " + type);
        return Optional.empty();
      }
    } catch (final Exception e) {
      LOG.error("Could not load TOML file", e);
      return Optional.empty();
    }
  }

  private Optional<FileBasedSigningMetadataFile> getFileBasedSigningMetadataFromToml(
      String filename, TomlParseResult result) {
    final String keyFilename = result.getTable("signing").getString("key-file");
    final Path keyPath = new File(keyFilename).toPath();
    final String passwordFilename = result.getTable("signing").getString("password-file");
    final Path passwordPath = new File(passwordFilename).toPath();
    return Optional.of(new FileBasedSigningMetadataFile(filename, keyPath, passwordPath));
  }

  private String normalizeAddress(final String address) {
    if (address.startsWith("0x")) {
      return address.replace("0x", "").toLowerCase();
    } else {
      return address.toLowerCase();
    }
  }
}

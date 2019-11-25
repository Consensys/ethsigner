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

  public static final String FILE_BASED_SIGNER = "file-based-signer";
  private static final Logger LOG = LogManager.getLogger();

  private static final String CONFIG_FILE_EXTENSION = ".toml";
  private static final String GLOB_CONFIG_MATCHER = "**" + CONFIG_FILE_EXTENSION;

  private final Path tomlConfigsDirectory;

  SigningMetadataTomlConfigLoader(final Path metadataTomlFilesDirectory) {
    this.tomlConfigsDirectory = metadataTomlFilesDirectory;
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
        getMetadataInfo(file).ifPresent(metadataFile -> metadataConfigs.add(metadataFile));
      }
      return metadataConfigs;
    } catch (final IOException e) {
      LOG.warn("Error searching for signing metadata TOML files", e);
      return Collections.emptySet();
    }
  }

  Optional<FileBasedSigningMetadataFile> getMetadataInfo(Path file) {
    try {
      TomlParseResult result =
          TomlConfigFileParser.loadConfigurationFromFile(file.toAbsolutePath().toString());
      String type = result.getTable("signing").getString("type");
      if (FILE_BASED_SIGNER.equals(type)) {
        String keyFilename = result.getTable("signing").getString("key-file");
        String passwordFilename = result.getTable("signing").getString("password-file");
        return Optional.of(
            new FileBasedSigningMetadataFile(
                file, new File(keyFilename).toPath(), new File(passwordFilename).toPath()));
      } else {
        LOG.error("unknown signing type in metadata %s", type);
        return Optional.empty();
      }
    } catch (Exception e) {
      LOG.error("could not load TOML file");
      return null;
    }
  }

  private String normalizeAddress(final String address) {
    if (address.startsWith("0x")) {
      return address.replace("0x", "").toLowerCase();
    } else {
      return address.toLowerCase();
    }
  }
}

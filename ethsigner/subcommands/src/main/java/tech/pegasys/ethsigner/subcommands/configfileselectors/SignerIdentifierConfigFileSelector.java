/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.ethsigner.subcommands.configfileselectors;

import tech.pegasys.signers.secp256k1.api.FileSelector;
import tech.pegasys.signers.secp256k1.api.SignerIdentifier;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;

import com.google.common.io.Files;

public class SignerIdentifierConfigFileSelector implements FileSelector<SignerIdentifier> {

  @Override
  public DirectoryStream.Filter<Path> getConfigFilesFilter(
      final SignerIdentifier signerIdentifier) {
    return entry -> matchesIdentifierInPath(signerIdentifier.toStringIdentifier(), entry);
  }

  private boolean matchesIdentifierInPath(final String identifier, final Path entry) {
    return fileNameEndsWith(identifier, entry) && hasTomlExtension(entry);
  }

  private boolean fileNameEndsWith(final String fileName, final Path entry) {
    return Files.getNameWithoutExtension(entry.getFileName().toString()).endsWith(fileName);
  }

  private boolean hasTomlExtension(final Path entry) {
    return Files.getFileExtension(entry.toString()).equals("toml");
  }
}

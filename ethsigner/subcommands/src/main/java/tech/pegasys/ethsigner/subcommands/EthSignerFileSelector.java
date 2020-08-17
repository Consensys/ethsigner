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
package tech.pegasys.ethsigner.subcommands;

import tech.pegasys.signers.secp256k1.EthPublicKeyUtils;
import tech.pegasys.signers.secp256k1.api.FileSelector;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.security.interfaces.ECPublicKey;

import com.google.common.io.Files;
import org.web3j.crypto.Keys;

public class EthSignerFileSelector implements FileSelector<ECPublicKey> {

  final String fileExtension = "toml";

  private boolean hasExpectedFileExtension(final Path entry) {
    return Files.getFileExtension(entry.toString()).equals(fileExtension);
  }

  @Override
  public Filter<Path> getAllConfigFilesFilter() {
    return this::hasExpectedFileExtension;
  }

  @Override
  public Filter<Path> getSpecificConfigFileFilter(final ECPublicKey publicKey) {
    return entry -> matchesPublicKey(publicKey, entry);
  }

  public boolean matchesPublicKey(final ECPublicKey publicKey, final Path entry)
      throws IOException {
    String addressToMatch = Keys.getAddress(EthPublicKeyUtils.toHexString(publicKey));

    return Files.getNameWithoutExtension(entry.getFileName().toString()).endsWith(addressToMatch)
        && hasExpectedFileExtension(entry);
  }
}

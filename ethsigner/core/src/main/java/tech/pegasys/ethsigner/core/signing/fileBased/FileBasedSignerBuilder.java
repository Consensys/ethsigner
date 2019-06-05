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
package tech.pegasys.ethsigner.core.signing.fileBased;

import tech.pegasys.ethsigner.core.signing.CredentialTransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.io.IOException;
import java.nio.file.Files;

import com.google.common.base.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

public class FileBasedSignerBuilder {

  private static final Logger LOG = LogManager.getLogger();
  private final FileBasedSignerConfig config;

  public FileBasedSignerBuilder(final FileBasedSignerConfig config) {
    this.config = config;
  }

  public TransactionSigner build() {
    String password;
    try {
      password = readPasswordFromFile(config);
    } catch (IOException e) {
      LOG.error(
          "Error when reading the password from file using the following path:\n {}.",
          config.getPasswordFilePath(),
          e);
      return null;
    }
    Credentials credentials;
    try {
      credentials = WalletUtils.loadCredentials(password, config.getKeyPath().toFile());
    } catch (IOException e) {
      LOG.error(
          "Error when reading key file for the file based signer using the following path:\n {}.",
          config.getKeyPath(),
          e);
      return null;
    } catch (CipherException e) {
      LOG.error(
          "Error when decrypting key for the file based signer using the following config:\n {}.",
          config.toString(),
          e);
      return null;
    }
    return new CredentialTransactionSigner(credentials);
  }

  private static String readPasswordFromFile(final FileBasedSignerConfig config)
      throws IOException {
    byte[] fileContent = Files.readAllBytes(config.getPasswordFilePath());
    return new String(fileContent, Charsets.UTF_8);
  }
}

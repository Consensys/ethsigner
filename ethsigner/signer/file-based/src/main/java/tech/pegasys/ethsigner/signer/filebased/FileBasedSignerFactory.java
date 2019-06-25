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
package tech.pegasys.ethsigner.signer.filebased;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerInitializationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

public class FileBasedSignerFactory {

  private static final Logger LOG = LogManager.getLogger();
  private static final String READ_PWD_FILE_MESSAGE = "Error when reading the password from file.";
  private static final String READ_AUTH_FILE_MESSAGE =
      "Error when reading key file for the file based signer.";
  private static final String DECRYPTING_KEY_FILE_MESSAGE =
      "Error when decrypting key for the file based signer.";

  public static TransactionSigner createSigner(
      final Path keyFilePath, final Path passwordFilePath) {
    final String password;
    try {
      password = readPasswordFromFile(passwordFilePath);
    } catch (final IOException e) {
      final String message = READ_PWD_FILE_MESSAGE;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    }
    try {
      final Credentials credentials = WalletUtils.loadCredentials(password, keyFilePath.toFile());
      return new CredentialTransactionSigner(credentials);
    } catch (final IOException e) {
      final String message = READ_AUTH_FILE_MESSAGE + keyFilePath.toString();
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    } catch (final CipherException e) {
      final String message = DECRYPTING_KEY_FILE_MESSAGE;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    }
  }

  private static String readPasswordFromFile(final Path path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(path);
    return new String(fileContent, Charsets.UTF_8);
  }
}

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
package tech.pegasys.ethsigner.core.signing.filebased;

import tech.pegasys.ethsigner.core.signing.CredentialTransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerConfig;
import tech.pegasys.ethsigner.core.signing.TransactionSignerInitializationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.Charsets;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.WalletUtils;

public class FileBasedTransactionSigner extends CredentialTransactionSigner {

  private static final Logger LOG = LogManager.getLogger();
  private static final String READ_PWD_FILE_MESSAGE =
      "Error when reading the password from file using the following path: ";
  private static final String READ_AUTH_FILE_MESSAGE =
      "Error when reading key file for the file based signer using the following path: ";
  private static final String DECRYPTING_KEY_FILE_MESSAGE =
      "Error when decrypting key for the file based signer using the following config:\n";

  public FileBasedTransactionSigner(final TransactionSignerConfig config) {
    final JsonObject jsonObject = new JsonObject(config.jsonString());
    final String password;
    final String passwordFilePathString = jsonObject.getString("passwordFilePath");
    try {
      password = readPasswordFromFile(passwordFilePathString);
    } catch (final IOException e) {
      final String message = READ_PWD_FILE_MESSAGE + passwordFilePathString;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    }
    final String keyFileString = jsonObject.getString("keyFilePath");
    try {
      this.credentials = WalletUtils.loadCredentials(password, new File(keyFileString));
    } catch (final IOException e) {
      final String message = READ_AUTH_FILE_MESSAGE + keyFileString;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    } catch (final CipherException e) {
      final String message = DECRYPTING_KEY_FILE_MESSAGE + config;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    }
  }

  private static String readPasswordFromFile(final String path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(Paths.get(path));
    return new String(fileContent, Charsets.UTF_8);
  }
}

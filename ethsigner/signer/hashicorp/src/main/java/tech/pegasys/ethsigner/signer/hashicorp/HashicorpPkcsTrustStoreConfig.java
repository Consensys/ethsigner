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
package tech.pegasys.ethsigner.signer.hashicorp;

import java.nio.file.Path;

import picocli.CommandLine;

class HashicorpPkcsTrustStoreConfig implements PkcsTrustStoreConfig {
  private static final String MANDATORY_FILE_FORMAT_HELP = "<FILE>";

  @CommandLine.Option(
      names = "--tls-truststore-file",
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      description =
          "Path to a PKCS#12 formatted trust store, containing all trusted root certificates.",
      arity = "1",
      required = true)
  private Path trustStoreFile;

  @CommandLine.Option(
      names = "--tls-truststore-password-file",
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      description = "Path to a file containing the password used to decrypt the trust store.",
      arity = "1",
      required = true)
  private Path trustStorePasswordFile;

  @Override
  public Path getPath() {
    return trustStoreFile;
  }

  @Override
  public Path getPasswordFilePath() {
    return trustStorePasswordFile;
  }
}

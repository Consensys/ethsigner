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
package tech.pegasys.ethsigner.config;

import tech.pegasys.ethsigner.core.config.PkcsStoreConfig;

import java.io.File;

import picocli.CommandLine.Option;

public class PicoCliDownstreamTlsClientAuthOptions implements PkcsStoreConfig {

  @Option(
      names = "--downstream-http-tls-keystore-file",
      description =
          "Path to a PKCS#12 formatted keystore, contains key/certificate to present to "
              + "a TLS-enabled web3 provider that requires client authentication.",
      arity = "1",
      required = true)
  private File clientCertificateFile;

  @Option(
      names = "--downstream-http-tls-keystore-password-file",
      description = "Path to a file containing the password used to decrypt the keystore.",
      arity = "1",
      required = true)
  private File clientCertificatePasswordFile;

  @Override
  public File getStoreFile() {
    return clientCertificateFile;
  }

  @Override
  public File getStorePasswordFile() {
    return clientCertificatePasswordFile;
  }
}

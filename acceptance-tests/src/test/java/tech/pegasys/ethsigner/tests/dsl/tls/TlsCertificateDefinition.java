/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.ethsigner.tests.dsl.tls;

import com.google.common.io.Resources;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import tech.pegasys.ethsigner.tests.dsl.ClientConfig;

public class TlsCertificateDefinition {

  private File certificateFile;
  private File pkcs12File;
  private String password;

  private static String resourcePath(final String subPath, final String filename) {
    return Path.of(subPath, filename).toString();
  }

  public static TlsCertificateDefinition loadFromResource(final String resourceSubPath,
      final String password) {
    try {
      final URL sslCertificate =
          Resources.getResource(resourcePath(resourceSubPath, "cert.pfx"));
      final Path keystorePath = Path.of(sslCertificate.getPath());

      final File certificateFile =
          Path.of(Resources.getResource(resourcePath(resourceSubPath, "cert.crt")).toURI())
              .toFile();

      return new TlsCertificateDefinition(keystorePath.toFile(), certificateFile, password);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to load TLS certificates", e);
    }
  }

  public TlsCertificateDefinition(final File pkcs12File, final File certificateFile,
      final String password) {
    this.certificateFile = certificateFile;
    this.pkcs12File = pkcs12File;
    this.password = password;
  }

  public File getCertificateFile() {
    return certificateFile;
  }

  public File getPkcs12File() {
    return pkcs12File;
  }

  public String getPassword() {
    return password;
  }
}

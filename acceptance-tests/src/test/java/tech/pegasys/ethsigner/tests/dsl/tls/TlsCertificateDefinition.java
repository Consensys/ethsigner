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
package tech.pegasys.ethsigner.tests.dsl.tls;

import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.loadP12KeyStore;

import com.google.common.collect.Lists;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import com.google.common.io.Resources;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

public class TlsCertificateDefinition {

  private File pkcs12File;
  private String password;

  public static TlsCertificateDefinition loadFromResource(
      final String resourcePath, final String password) {
    try {
      final URL sslCertificate = Resources.getResource(resourcePath);
      final Path keystorePath = Path.of(sslCertificate.getPath());

      return new TlsCertificateDefinition(keystorePath.toFile(), password);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to load TLS certificates", e);
    }
  }

  public TlsCertificateDefinition(final File pkcs12File, final String password) {
    this.pkcs12File = pkcs12File;
    this.password = password;
  }

  public File getPkcs12File() {
    return pkcs12File;
  }

  public String getPassword() {
    return password;
  }

  public List<X509Certificate> certificates()
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
    final List<X509Certificate> results = Lists.newArrayList();

    final KeyStore p12 = loadP12KeyStore(pkcs12File, password);
    final Enumeration<String> aliases = p12.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      results.add((X509Certificate) p12.getCertificate(alias));
    }
    return results;
  }

  public List<PrivateKey> keys()
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
    final List<PrivateKey> results = Lists.newArrayList();

    final KeyStore p12 = loadP12KeyStore(pkcs12File, password);
    final Enumeration<String> aliases = p12.aliases();
    Key theKey;
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      results.add((PrivateKey) p12.getKey(alias, password.toCharArray()));
    }
    return results;
  }

}

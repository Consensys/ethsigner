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
package tech.pegasys.ethsigner.tests;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.SelfSignedCertificate;

public class VertxTlsUtil {

  public static PfxOptions convertToPfxKeyStore(
      final SelfSignedCertificate selfSignedCertificate, final char[] password)
      throws IOException, GeneralSecurityException {
    return convert(selfSignedCertificate, password, true);
  }

  public static PfxOptions convertToPfxTrustStore(
      final SelfSignedCertificate selfSignedCertificate, final char[] password)
      throws IOException, GeneralSecurityException {
    return convert(selfSignedCertificate, password, false);
  }

  private static PfxOptions convert(
      final SelfSignedCertificate selfSignedCertificate,
      final char[] password,
      final boolean isKeyStore)
      throws IOException, GeneralSecurityException {
    final Certificate certificate = getCertificate(selfSignedCertificate.certificatePath());
    final KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null);
    if (isKeyStore) {
      final PrivateKey privateKey = getPrivateKey(selfSignedCertificate.privateKeyPath());
      keyStore.setKeyEntry("test", privateKey, password, new Certificate[] {certificate});
    } else {
      keyStore.setCertificateEntry("testca", certificate);
    }

    final Path pfxPath = saveKeyStore(password, keyStore);
    return new PfxOptions().setPath(pfxPath.toString()).setPassword(new String(password));
  }

  private static PrivateKey getPrivateKey(final String pemPrivateKeyPath)
      throws IOException, GeneralSecurityException {
    // remove private key headers/footers/end-of-lines
    final String pemEncodedPrivateKey =
        Files.readString(Paths.get(pemPrivateKeyPath), StandardCharsets.UTF_8)
            .replace("-----BEGIN PRIVATE KEY-----\n", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\n", "");

    final byte[] derPrivateKey = Base64.getDecoder().decode(pemEncodedPrivateKey);
    final PKCS8EncodedKeySpec derKeySpec = new PKCS8EncodedKeySpec(derPrivateKey);
    final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(derKeySpec);
  }

  private static Certificate getCertificate(final String pemCertificatePath)
      throws IOException, GeneralSecurityException {
    final byte[] pemCertificate = Files.readAllBytes(Paths.get(pemCertificatePath));
    final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    return certificateFactory.generateCertificate(new ByteArrayInputStream(pemCertificate));
  }

  private static Path saveKeyStore(final char[] password, final KeyStore keyStore)
      throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
    // Set<PosixFilePermission> ownerWritable = PosixFilePermissions.fromString("rw-r--r--");
    // FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(ownerWritable);
    final Path pfxPath = Files.createTempFile("test", ".pfx");
    try (FileOutputStream outputStream = new FileOutputStream(pfxPath.toFile())) {
      keyStore.store(outputStream, password);
    }
    return pfxPath;
  }
}

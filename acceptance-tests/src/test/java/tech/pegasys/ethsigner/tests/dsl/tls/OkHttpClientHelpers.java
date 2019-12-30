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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.StringJoiner;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class OkHttpClientHelpers {

  public static OkHttpClient createOkHttpClient(
      final Optional<ClientTlsConfig> clientTlsConfiguration) {
    final OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(10));

    if (clientTlsConfiguration.isPresent()) {
      final ClientTlsConfig clientTlsConfig = clientTlsConfiguration.get();
      try {
        final KeyManager[] keyManagers =
            createKeyManagers(clientTlsConfig.getClientCertificateToPresent());

        final TrustManagerFactory trustManagerFactory =
            createTrustManagerFactory(clientTlsConfig.getExpectedTlsServerCert());

        final X509TrustManager trustManager =
            (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            keyManagers, trustManagerFactory.getTrustManagers(), SecureRandom.getInstanceStrong());

        clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
      } catch (final KeyStoreException | UnrecoverableKeyException e) {
        fail("Unable to construct a TLS client during test setup, failed to setup keystore.", e);
      } catch (final NoSuchAlgorithmException e) {
        fail("Unable to construct a TLS client during test setup, missing encryption algorithm.");
      } catch (final KeyManagementException e) {
        fail("Unable to construct a TLS client during test setup due to KeyManagementException");
      } catch (final CertificateException e) {
        fail("Unable to construct a X509 certificate for EthSigner client, using supplied file.");
      }
    }

    return clientBuilder.build();
  }

  private static KeyManager[] createKeyManagers(final TlsCertificateDefinition certToPresent)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
      UnrecoverableKeyException {
    if (certToPresent == null) {
      return null;
    }

    final String password = certToPresent.getPassword();

    final KeyStore clientCertStore = loadP12KeyStore(certToPresent.getPkcs12File(), password);

    final KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
    kmf.init(clientCertStore, password.toCharArray());
    return kmf.getKeyManagers();
  }

  private static TrustManagerFactory createTrustManagerFactory(
      final TlsCertificateDefinition serverCert)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException {

    final KeyStore trustStore =
        loadP12KeyStore(serverCert.getPkcs12File(), serverCert.getPassword());
    final TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);

    return trustManagerFactory;
  }

  private static KeyStore loadP12KeyStore(final File pkcsFile, final String password)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
    final KeyStore store = KeyStore.getInstance("pkcs12");
    try (final InputStream keystoreStream = new FileInputStream(pkcsFile)) {
      store.load(keystoreStream, password.toCharArray());
    } catch (IOException e) {
      throw new RuntimeException("Unable to load keystore.", e);
    }
    return store;
  }

  public static void populateFingerprintFile(
      final Path knownClientsPath, final File certificateFile)
      throws IOException, NoSuchAlgorithmException, CertificateException {
    final InputStream is = new FileInputStream(certificateFile);
    final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    final X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(is);

    final String fingerprint = generateFingerprint(cert);
    Files.writeString(
        knownClientsPath, "localhost " + fingerprint + "\n" + "127.0.0.1 " + fingerprint + "\n");
  }

  private static String generateFingerprint(final X509Certificate cert)
      throws NoSuchAlgorithmException, CertificateEncodingException {
    final MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(cert.getEncoded());
    final byte[] digest = md.digest();

    final StringJoiner joiner = new StringJoiner(":");
    for (final byte b : digest) {
      joiner.add(String.format("%02X", b));
    }

    return joiner.toString().toLowerCase();
  }
}

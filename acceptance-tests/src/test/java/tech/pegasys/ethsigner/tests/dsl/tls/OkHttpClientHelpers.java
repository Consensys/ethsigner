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
import tech.pegasys.ethsigner.tests.dsl.ClientConfig;

public class OkHttpClientHelpers {

  public static OkHttpClient createOkHttpClient(
      final Optional<ClientConfig> clientTlsConfiguration) {
    final OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(10));

    if (clientTlsConfiguration.isPresent()) {
      try {
        final KeyManager[] keyManagers;
        if (clientTlsConfiguration.get().getClientCertificateToPresent() != null) {
          final TlsCertificateDefinition clientCert =
              clientTlsConfiguration.get().getClientCertificateToPresent();
          final KeyStore clientCertStore = loadP12KeyStore(clientCert.getPkcs12File(),
              clientCert.getPassword().toCharArray());
          final KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
          kmf.init(clientCertStore, clientCert.getPassword().toCharArray());
          keyManagers = kmf.getKeyManagers();
        } else {
          keyManagers = null;
        }

        final TlsCertificateDefinition serverCert =
            clientTlsConfiguration.get().getExpectedTlsServerCert();
        final KeyStore trustStore =
            loadP12KeyStore(serverCert.getPkcs12File(), serverCert.getPassword().toCharArray());
        final TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        final X509TrustManager trustManager =
            (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext
            .init(keyManagers, trustManagerFactory.getTrustManagers(), SecureRandom.getInstanceStrong());

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

  private static KeyStore loadP12KeyStore(final File pkcsFile, final char[] password)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
    final KeyStore store = KeyStore.getInstance("pkcs12");
    try (final InputStream keystoreStream = new FileInputStream(pkcsFile)) {
      store.load(keystoreStream, password);
    } catch (IOException e) {
      throw new RuntimeException("Unable to load keystore.", e);
    }
    return store;
  }

  public static void generateClientFingerPrint(final Path knownClientsPath,
      final File certificateFile)
      throws IOException, NoSuchAlgorithmException, CertificateException {
    FileInputStream is = new FileInputStream(certificateFile);
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(is);

    final String fingerprint = getFingerprint(cert);
    Files.writeString(knownClientsPath, "localhost " + fingerprint);
  }

  private static String getFingerprint(final X509Certificate cert)
      throws NoSuchAlgorithmException, CertificateEncodingException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] der = cert.getEncoded();
    md.update(der);
    byte[] digest = md.digest();

    final StringJoiner joiner = new StringJoiner(":");
    for (final byte b : digest) {
      joiner.add(String.format("%02X", b));
    }

    return joiner.toString().toLowerCase();

  }
}

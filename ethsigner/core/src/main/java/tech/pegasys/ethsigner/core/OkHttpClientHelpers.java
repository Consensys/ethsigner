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
package tech.pegasys.ethsigner.core;

import tech.pegasys.ethsigner.core.config.PkcsStoreConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.google.common.base.Charsets;
import okhttp3.OkHttpClient;

public class OkHttpClientHelpers {

  public static OkHttpClient applyTlsConfigTo(
      final OkHttpClient input,
      final Optional<PkcsStoreConfig> clientCertificate,
      final Optional<PkcsStoreConfig> serverTrustStore) {

    final OkHttpClient.Builder clientBuilder = input.newBuilder();

    new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(10));

    try {
      final KeyManager[] keyManagers;
      final TrustManager[] trustManagers;

      final X509TrustManager trustManager;
      if (clientCertificate.isPresent()) {
        keyManagers = createKeyManagers(clientCertificate.get());
      } else {
        keyManagers = null;
      }

      if (serverTrustStore.isPresent()) {
        final TrustManagerFactory trustManagerFactory =
            createTrustManagerFactory(serverTrustStore.get());
        trustManagers = trustManagerFactory.getTrustManagers();
        trustManager = (X509TrustManager) trustManagers[0];

      } else {
        trustManagers = null;
        // Trust all servers
        trustManager =
            new X509TrustManager() {
              @Override
              public void checkClientTrusted(
                  final X509Certificate[] chain, final String authType) {}

              @Override
              public void checkServerTrusted(
                  final X509Certificate[] chain, final String authType) {}

              @Override
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }
            };
      }

      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, trustManagers, SecureRandom.getInstanceStrong());

      clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

    } catch (final KeyStoreException | UnrecoverableKeyException e) {
      throw new InitializationException(
          "Unable to construct a TLS client during test setup, failed to setup keystore.", e);
    } catch (final NoSuchAlgorithmException e) {
      throw new InitializationException(
          "Unable to construct a TLS client during test setup, missing encryption algorithm.");
    } catch (final KeyManagementException e) {
      throw new InitializationException(
          "Unable to construct a TLS client during test setup due to KeyManagementException");
    } catch (final CertificateException e) {
      throw new InitializationException(
          "Unable to construct a X509 certificate for EthSigner client, using supplied file.");
    } catch (final IOException e) {
      throw new InitializationException("Unable to load TLS Keystore/certificate", e);
    }

    return clientBuilder.build();
  }

  private static KeyManager[] createKeyManagers(final PkcsStoreConfig certToPresent)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
          UnrecoverableKeyException, IOException {
    if (certToPresent == null) {
      return null;
    }
    final String password = readSecretFromFile(certToPresent.getStorePasswordFile().toPath());

    final KeyStore clientCertStore = loadP12KeyStore(certToPresent.getStoreFile(), password);

    final KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
    kmf.init(clientCertStore, password.toCharArray());
    return kmf.getKeyManagers();
  }

  private static TrustManagerFactory createTrustManagerFactory(final PkcsStoreConfig serverCert)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

    final String password = readSecretFromFile(serverCert.getStorePasswordFile().toPath());
    final KeyStore trustStore = loadP12KeyStore(serverCert.getStoreFile(), password);
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

  private static String readSecretFromFile(final Path path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(path);
    return new String(fileContent, Charsets.UTF_8);
  }
}

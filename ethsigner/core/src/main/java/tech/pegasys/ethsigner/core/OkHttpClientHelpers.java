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
        // use default trust store
        trustManagers = null;
        trustManager = defaultTrustManager();
      }

      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, trustManagers, null);

      clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

    } catch (final KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
      throw new InitializationException(
          "Failed to initialize TLS on the link to downstream web3 provider.", e);
    }

    return clientBuilder.build();
  }

  private static X509TrustManager defaultTrustManager()
      throws NoSuchAlgorithmException, KeyStoreException {
    final TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    trustManagerFactory.init((KeyStore) null);

    System.out.println("JVM Default Trust Managers:");
    for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
      System.out.println(trustManager);

      if (trustManager instanceof X509TrustManager) {
        return (X509TrustManager) trustManager;
      }
    }
    throw new InitializationException("Unable to find the default X509 trust manager.");
  }

  private static KeyManager[] createKeyManagers(final PkcsStoreConfig certToPresent) {
    if (certToPresent == null) {
      return null;
    }

    final String password;
    try {
      password = readSecretFromFile(certToPresent.getStorePasswordFile().toPath());
    } catch (final IOException e) {
      throw new InitializationException("Failed to load web3 client certificate password file", e);
    }

    try {
      final KeyStore clientCertStore = loadP12KeyStore(certToPresent.getStoreFile(), password);

      final KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
      kmf.init(clientCertStore, password.toCharArray());
      return kmf.getKeyManagers();
    } catch (final KeyStoreException e) {
      throw new InitializationException(
          "Failed to load the PKCS#12 Keystore for client certificate presentation.", e);
    } catch (final NoSuchAlgorithmException e) {
      throw new InitializationException("KeyManagerFactory cannot be found for PKIX", e);
    } catch (UnrecoverableKeyException | CertificateException e) {
      throw new InitializationException("Certificate unable to be read from keystore", e);
    } catch (final IOException e) {
      throw new InitializationException("Unable to load supplied client keystore", e);
    }
  }

  private static TrustManagerFactory createTrustManagerFactory(final PkcsStoreConfig serverCert) {

    final String password;
    try {
      password = readSecretFromFile(serverCert.getStorePasswordFile().toPath());
    } catch (final IOException e) {
      throw new InitializationException("Failed to load web3 truststore password file", e);
    }

    try {
      final KeyStore trustStore = loadP12KeyStore(serverCert.getStoreFile(), password);
      final TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);
      return trustManagerFactory;
    } catch (final KeyStoreException e) {
      throw new InitializationException(
          "Failed to load the PKCS#12 Keystore for web3 authenticating truststore.", e);
    } catch (final NoSuchAlgorithmException e) {
      throw new InitializationException("Default TrustManagerFactory cannot be found.", e);
    } catch (CertificateException e) {
      throw new InitializationException("Certificate unable to be read from keystore", e);
    } catch (final IOException e) {
      throw new InitializationException("Failed to load the Web3 Trust store file", e);
    }
  }

  private static KeyStore loadP12KeyStore(final File pkcsFile, final String password)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    final KeyStore store = KeyStore.getInstance("pkcs12");
    final InputStream keystoreStream = new FileInputStream(pkcsFile);
    store.load(keystoreStream, password.toCharArray());
    return store;
  }

  private static String readSecretFromFile(final Path path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(path);
    return new String(fileContent, Charsets.UTF_8);
  }
}

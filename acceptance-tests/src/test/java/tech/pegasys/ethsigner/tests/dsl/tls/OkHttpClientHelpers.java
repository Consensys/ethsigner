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
import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.createKeyManagers;
import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.createTrustManagerFactory;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Optional;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class OkHttpClientHelpers {

  public static OkHttpClient createOkHttpClient(
      final Optional<ClientTlsConfig> clientTlsConfiguration) {

    final int secondsToWait = Boolean.getBoolean("debugSubProcess") ? 3600 : 10;

    final OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(secondsToWait));

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
        sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), new SecureRandom());

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
}

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
import java.io.FileNotFoundException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import okhttp3.OkHttpClient;

public class OkHttpClientHelpers {

  public static OkHttpClient createOkHttpClient(final Optional<File> expectedCertificate) {
    final OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(2));

    if (expectedCertificate.isPresent()) {

      try {
        final FileInputStream myTrustedCAFileContent =
            new FileInputStream(expectedCertificate.get());
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final X509Certificate myCAPublicKey =
            (X509Certificate) certificateFactory.generateCertificate(myTrustedCAFileContent);

        final MatchingCertTrustManager insecureTrustManager =
            new MatchingCertTrustManager(myCAPublicKey);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {insecureTrustManager}, new SecureRandom());
        clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), insecureTrustManager);
      } catch (final NoSuchAlgorithmException e) {
        fail("Unable to construct a TLS client during test setup, missing encryption algorithm.");
      } catch (final KeyManagementException e) {
        fail("Unable to construct a TLS client during test setup due to KeyManagementException");
      } catch (final CertificateException e) {
        fail("Unable to construct a X509 certificate for EthSigner client, using supplied file.");
      } catch (final FileNotFoundException e) {
        fail("Unable to construct a TLS client during test setup, certificate file is missing.");
      }
    }

    return clientBuilder.build();
  }
}

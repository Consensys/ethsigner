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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class MatchingCertTrustManager implements X509TrustManager {

  final X509Certificate trustedCertificate;

  public MatchingCertTrustManager(X509Certificate trustedCertificate) {
    this.trustedCertificate = trustedCertificate;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {}

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {}

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[] {trustedCertificate};
  }
}

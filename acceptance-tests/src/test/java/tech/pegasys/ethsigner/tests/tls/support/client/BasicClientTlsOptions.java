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
package tech.pegasys.ethsigner.tests.tls.support.client;

import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsCertificateOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsTrustOptions;

import java.util.Optional;

public class BasicClientTlsOptions implements ClientTlsOptions {
  private final boolean isTlsEnabled;
  private final Optional<ClientTlsCertificateOptions> clientTlsCertificateOptions;
  private final Optional<ClientTlsTrustOptions> clientTlsTrustOptions;

  public BasicClientTlsOptions(
      final boolean isTlsEnabled,
      final ClientTlsCertificateOptions clientTlsCertificateOptions,
      final ClientTlsTrustOptions clientTlsTrustOptions) {
    this.isTlsEnabled = isTlsEnabled;
    this.clientTlsCertificateOptions = Optional.ofNullable(clientTlsCertificateOptions);
    this.clientTlsTrustOptions = Optional.ofNullable(clientTlsTrustOptions);
  }

  @Override
  public boolean isTlsEnabled() {
    return isTlsEnabled;
  }

  @Override
  public Optional<ClientTlsCertificateOptions> getClientTlsCertificateOptions() {
    return clientTlsCertificateOptions;
  }

  @Override
  public Optional<ClientTlsTrustOptions> getClientTlsTrustOptions() {
    return clientTlsTrustOptions;
  }
}

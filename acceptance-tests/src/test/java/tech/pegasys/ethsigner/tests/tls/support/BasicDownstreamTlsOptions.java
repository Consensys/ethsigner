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
package tech.pegasys.ethsigner.tests.tls.support;

import tech.pegasys.ethsigner.core.config.DownstreamTlsOptions;
import tech.pegasys.ethsigner.core.config.DownstreamTrustOptions;
import tech.pegasys.ethsigner.core.config.PkcsStoreConfig;

import java.util.Objects;
import java.util.Optional;

public class BasicDownstreamTlsOptions implements DownstreamTlsOptions {
  private final boolean isTlsEnabled;
  private final Optional<PkcsStoreConfig> downstreamTlsClientAuthOptions;
  private final Optional<DownstreamTrustOptions> downstreamTlsTrustOptions;

  public BasicDownstreamTlsOptions(
      final boolean isTlsEnabled,
      final Optional<PkcsStoreConfig> downstreamTlsClientAuthOptions,
      final Optional<DownstreamTrustOptions> downstreamTlsTrustOptions) {
    Objects.requireNonNull(downstreamTlsClientAuthOptions);
    Objects.requireNonNull(downstreamTlsTrustOptions);
    this.isTlsEnabled = isTlsEnabled;
    this.downstreamTlsClientAuthOptions = downstreamTlsClientAuthOptions;
    this.downstreamTlsTrustOptions = downstreamTlsTrustOptions;
  }

  @Override
  public boolean isTlsEnabled() {
    return isTlsEnabled;
  }

  @Override
  public Optional<PkcsStoreConfig> getDownstreamTlsClientAuthOptions() {
    return downstreamTlsClientAuthOptions;
  }

  @Override
  public Optional<DownstreamTrustOptions> getDownstreamTlsServerTrustOptions() {
    return downstreamTlsTrustOptions;
  }
}

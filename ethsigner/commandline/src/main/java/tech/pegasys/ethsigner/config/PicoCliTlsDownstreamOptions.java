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
package tech.pegasys.ethsigner.config;

import tech.pegasys.ethsigner.core.config.DownstreamTlsOptions;
import tech.pegasys.ethsigner.core.config.DownstreamTrustOptions;
import tech.pegasys.ethsigner.core.config.PkcsStoreConfig;

import java.util.Optional;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class PicoCliTlsDownstreamOptions implements DownstreamTlsOptions {
  @Option(
      names = "--downstream-http-tls-enabled",
      description = "Flag to enable TLS connection to web3 provider. Defaults to disabled",
      arity = "0",
      required = true,
      order = 15)
  private boolean tlsEnabled = false;

  @ArgGroup(exclusive = false)
  private PicoCliDownstreamTlsClientAuthOptions downstreamTlsClientAuthOptions;

  @ArgGroup(exclusive = false)
  private PicoCliDownstreamTlsTrustOptions downstreamServerTrustOptions;

  @Override
  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  @Override
  public Optional<PkcsStoreConfig> getDownstreamTlsClientAuthOptions() {
    return Optional.ofNullable(downstreamTlsClientAuthOptions);
  }

  @Override
  public Optional<DownstreamTrustOptions> getDownstreamTlsServerTrustOptions() {
    return Optional.ofNullable(downstreamServerTrustOptions);
  }
}

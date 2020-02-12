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
package tech.pegasys.ethsigner.tests.dsl.signer;

import tech.pegasys.ethsigner.core.config.TlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;

import java.time.Duration;
import java.util.Optional;

public class SignerConfiguration {

  private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  public static final int UNASSIGNED_PORT = 0;

  private final String chainId;
  private final String hostname;
  private final int httpRpcPort;
  private final int webSocketPort;
  private final TransactionSignerParamsSupplier transactionSignerParamsSupplier;
  private final Optional<TlsOptions> serverTlsOptions;
  private final Optional<ClientTlsOptions> clientTlsOptions;
  private final Optional<TlsCertificateDefinition> overriddenCaTrustStore;

  public SignerConfiguration(
      final String chainId,
      final String hostname,
      final int httpRpcPort,
      final int webSocketPort,
      final TransactionSignerParamsSupplier transactionSignerParamsSupplier,
      final Optional<TlsOptions> serverTlsOptions,
      final Optional<ClientTlsOptions> clientTlsOptions,
      final Optional<TlsCertificateDefinition> overriddenCaTrustStore) {
    this.chainId = chainId;
    this.hostname = hostname;
    this.httpRpcPort = httpRpcPort;
    this.webSocketPort = webSocketPort;
    this.transactionSignerParamsSupplier = transactionSignerParamsSupplier;
    this.serverTlsOptions = serverTlsOptions;
    this.clientTlsOptions = clientTlsOptions;
    this.overriddenCaTrustStore = overriddenCaTrustStore;
  }

  public String hostname() {
    return hostname;
  }

  public Duration pollingInterval() {
    return POLLING_INTERVAL;
  }

  public Duration timeout() {
    return TIMEOUT;
  }

  public String chainId() {
    return chainId;
  }

  public int httpRpcPort() {
    return httpRpcPort;
  }

  public int webSocketPort() {
    return webSocketPort;
  }

  public Optional<TlsOptions> serverTlsOptions() {
    return serverTlsOptions;
  }

  public Optional<ClientTlsOptions> clientTlsOptions() {
    return clientTlsOptions;
  }

  public Optional<TlsCertificateDefinition> getOverriddenCaTrustStore() {
    return overriddenCaTrustStore;
  }

  public TransactionSignerParamsSupplier transactionSignerParamsSupplier() {
    return transactionSignerParamsSupplier;
  }

  public boolean isDynamicPortAllocation() {
    return httpRpcPort == UNASSIGNED_PORT && webSocketPort == UNASSIGNED_PORT;
  }
}

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

import java.time.Duration;

public class SignerConfiguration {

  private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);
  public static final int UNASSIGNED_PORT = 0;

  private final String chainId;
  private final String hostname;
  private final int httpRpcPort;
  private final int webSocketPort;
  private final int hashicorpVaultPort;

  public SignerConfiguration(
      final String chainId,
      final String hostname,
      final int httpRpcPort,
      final int webSocketPort,
      final int hashicorpVaultPort) {
    this.chainId = chainId;
    this.hostname = hostname;
    this.httpRpcPort = httpRpcPort;
    this.webSocketPort = webSocketPort;
    this.hashicorpVaultPort = hashicorpVaultPort;
  }

  public String hostname() {
    return hostname;
  }

  public Duration pollingInterval() {
    return POLLING_INTERVAL;
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

  public int hashicorpVaultPort() {
    return hashicorpVaultPort;
  }

  public boolean isDynamicPortAllocation() {
    return httpRpcPort == UNASSIGNED_PORT && webSocketPort == UNASSIGNED_PORT;
  }
}

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
package tech.pegasys.ethsigner.tests.dsl.utils;

import tech.pegasys.ethsigner.signer.hashicorp.TrustStoreConfig;
import tech.pegasys.ethsigner.tests.hashicorpvault.HashicorpVaultDocker;

import java.util.Optional;

import com.github.dockerjava.api.DockerClient;

public class HashicorpVault {

  private final HashicorpVaultDocker dockerContainer;
  private final boolean tlsEnabled;
  private final TrustStoreConfig signerTrustConfig;

  private HashicorpVault(
      final HashicorpVaultDocker dockerContainer,
      final boolean tlsEnabled,
      final TrustStoreConfig signerTrustConfig) {
    this.dockerContainer = dockerContainer;
    this.tlsEnabled = tlsEnabled;
    this.signerTrustConfig = signerTrustConfig;
  }

  public static HashicorpVault createVault(final DockerClient docker) {
    final HashicorpVaultDocker hashicorpVaultDocker = new HashicorpVaultDocker(docker);
    hashicorpVaultDocker.start();
    hashicorpVaultDocker.awaitStartupCompletion();
    hashicorpVaultDocker.createTestData();
    return new HashicorpVault(hashicorpVaultDocker, false, null);
  }

  public static HashicorpVault createVaultWithTls(
      final DockerClient docker, final TrustStoreConfig trustStoreConfig) {
    final HashicorpVaultDocker hashicorpVaultDocker = new HashicorpVaultDocker(docker);
    hashicorpVaultDocker.start();
    hashicorpVaultDocker.awaitStartupCompletion();
    hashicorpVaultDocker.createTestData();
    return new HashicorpVault(hashicorpVaultDocker, true, trustStoreConfig);
  }

  public void shutdown() {
    if (dockerContainer != null) {
      dockerContainer.shutdown();
    }
  }

  public int getPort() {
    return dockerContainer.getPort();
  }

  public String getIpAddress() {
    return dockerContainer.getIpAddress();
  }

  public String getVaultToken() {
    return dockerContainer.getVaultToken();
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public Optional<TrustStoreConfig> getSignerTrustConfig() {
    return Optional.ofNullable(signerTrustConfig);
  }
}

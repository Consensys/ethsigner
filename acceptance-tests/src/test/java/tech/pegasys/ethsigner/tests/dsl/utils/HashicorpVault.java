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

import tech.pegasys.ethsigner.tests.hashicorpvault.HashicorpVaultDocker;

import com.github.dockerjava.api.DockerClient;

public class HashicorpVault {

  private final HashicorpVaultDocker dockerContainer;

  private HashicorpVault(final HashicorpVaultDocker dockerContainer) {
    this.dockerContainer = dockerContainer;
  }

  public static HashicorpVault createVault(final DockerClient docker) {
    final HashicorpVaultDocker hashicorpVaultDocker = new HashicorpVaultDocker(docker);
    hashicorpVaultDocker.start();
    hashicorpVaultDocker.awaitStartupCompletion();
    hashicorpVaultDocker.createTestData();
    return new HashicorpVault(hashicorpVaultDocker);
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
}

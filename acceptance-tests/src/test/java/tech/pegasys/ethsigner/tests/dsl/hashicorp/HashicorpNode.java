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
package tech.pegasys.ethsigner.tests.dsl.hashicorp;

import static com.google.common.io.Resources.getResource;

import tech.pegasys.ethsigner.signer.hashicorp.TrustStoreConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.github.dockerjava.api.DockerClient;

public class HashicorpNode {
  private static final Path TRUSTSTORE_PATH =
      Paths.get(getResource("tls/hashicorp/truststore.pfx").getPath());
  private static final Path TRUSTSTORE_PASSWORD_PATH =
      Paths.get(getResource("tls/hashicorp/truststore_password.txt").getPath());
  private final HashicorpVaultCerts hashicorpVaultCerts;
  private final DockerClient dockerClient;
  private HashicorpVaultDocker hashicorpVaultDocker;

  private HashicorpNode(
      final DockerClient dockerClient, final HashicorpVaultCerts hashicorpVaultCerts) {
    this.dockerClient = dockerClient;
    this.hashicorpVaultCerts = hashicorpVaultCerts;
  }

  public static HashicorpNode createAndStartHashicorp(final DockerClient dockerClient) {
    final HashicorpNode hashicorpNode = new HashicorpNode(dockerClient, new HashicorpVaultCerts());
    hashicorpNode.start();
    return hashicorpNode;
  }

  public static HashicorpNode createAndStartHashicorpWithoutTls(final DockerClient dockerClient) {
    final HashicorpNode hashicorpNode = new HashicorpNode(dockerClient, null);
    hashicorpNode.start();
    return hashicorpNode;
  }

  private void start() {
    hashicorpVaultDocker =
        HashicorpVaultDocker.createVaultDocker(dockerClient, hashicorpVaultCerts);
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  public void shutdown() {
    if (hashicorpVaultDocker != null) {
      hashicorpVaultDocker.shutdown();
    }
  }

  public String getVaultToken() {
    return hashicorpVaultDocker.getHashicorpRootToken();
  }

  public String getHost() {
    return hashicorpVaultDocker.getIpAddress();
  }

  public int getPort() {
    return hashicorpVaultDocker.getPort();
  }

  public boolean isTlsEnabled() {
    return hashicorpVaultCerts != null;
  }

  public Optional<TrustStoreConfig> getSignerTrustConfig() {
    if (!isTlsEnabled()) {
      return Optional.empty();
    }

    return Optional.of(
        new TrustStoreConfig() {
          @Override
          public Path getStoreFile() {
            return TRUSTSTORE_PATH;
          }

          @Override
          public Path getStorePasswordFile() {
            return TRUSTSTORE_PASSWORD_PATH;
          }
        });
  }
}

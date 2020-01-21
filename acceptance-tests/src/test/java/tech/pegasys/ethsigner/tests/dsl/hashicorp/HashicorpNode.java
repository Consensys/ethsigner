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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.github.dockerjava.api.DockerClient;
import org.apache.tuweni.net.tls.TLS;

public class HashicorpNode {
  private final HashicorpVaultDockerCertificate hashicorpVaultDockerCertificate;
  private final DockerClient dockerClient;
  private HashicorpVaultDocker hashicorpVaultDocker;
  private Optional<Path> knownServerFile = Optional.empty();

  private HashicorpNode(
      final DockerClient dockerClient,
      final HashicorpVaultDockerCertificate hashicorpVaultDockerCertificate) {
    this.dockerClient = dockerClient;
    this.hashicorpVaultDockerCertificate = hashicorpVaultDockerCertificate;
  }

  public static HashicorpNode createAndStartHashicorp(final DockerClient dockerClient) {
    final HashicorpNode hashicorpNode =
        new HashicorpNode(dockerClient, HashicorpVaultDockerCertificate.create());
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
        HashicorpVaultDocker.createVaultDocker(dockerClient, hashicorpVaultDockerCertificate);
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

    if (isTlsEnabled()) {
      knownServerFile = Optional.of(createKnownServerFile());
    }
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

  public String getSigningKeyPath() {
    return hashicorpVaultDocker.getVaultSigningKeyPath();
  }

  public int getPort() {
    return hashicorpVaultDocker.getPort();
  }

  public boolean isTlsEnabled() {
    return hashicorpVaultDockerCertificate != null;
  }

  public Optional<Path> getKnownServerFilePath() {
    return knownServerFile;
  }

  private Path createKnownServerFile() {
    try {
      final Path tempFile = Files.createTempFile("knownServer", ".txt");
      final String hexFingerprint =
          TLS.certificateHexFingerprint(hashicorpVaultDockerCertificate.getTlsCertificate());
      Files.writeString(tempFile, String.format("%s:%d %s", getHost(), getPort(), hexFingerprint));
      return tempFile;
    } catch (final IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }
}

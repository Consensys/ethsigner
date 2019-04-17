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
package tech.pegasys.ethsigner.tests.dsl;

import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;

import java.net.URI;
import java.util.Optional;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

public class ConfigurationFactory {

  private static final String LOCALHOST = "127.0.0.1";

  private final DockerClient docker;
  private final NodeConfiguration node;
  private final SignerConfiguration signer;

  public ConfigurationFactory() {
    final DefaultDockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    this.docker = createDockerClient(config);
    this.node = nodeConfiguration(config);
    this.signer = new SignerConfiguration(LOCALHOST);
  }

  private NodeConfiguration nodeConfiguration(final DefaultDockerClientConfig config) {
    final String hostname = dockerHost(config).orElse(LOCALHOST);

    return new NodeConfiguration(hostname);
  }

  private Optional<String> dockerHost(final DefaultDockerClientConfig config) {
    return Optional.of(config.getDockerHost()).map(URI::getHost);
  }

  private DockerClient createDockerClient(final DefaultDockerClientConfig config) {
    final DockerCmdExecFactory dockerCmdExecFactory =
        new JerseyDockerCmdExecFactory()
            .withReadTimeout(5000)
            .withConnectTimeout(30000)
            .withMaxTotalConnections(100)
            .withMaxPerRouteConnections(10);

    return DockerClientBuilder.getInstance(config)
        .withDockerCmdExecFactory(dockerCmdExecFactory)
        .build();
  }

  public DockerClient docker() {
    return docker;
  }

  public NodeConfiguration node() {
    return node;
  }

  public SignerConfiguration signer() {
    return signer;
  }
}

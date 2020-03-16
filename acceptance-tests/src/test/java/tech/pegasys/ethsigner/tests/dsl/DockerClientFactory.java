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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

public class DockerClientFactory {

  private final DefaultDockerClientConfig config;

  public DockerClientFactory() {
    this.config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
  }

  private DockerClient createDockerClient(final DefaultDockerClientConfig config) {
    final DockerCmdExecFactory dockerCmdExecFactory =
        new JerseyDockerCmdExecFactory()
            .withMaxTotalConnections(100)
            .withMaxPerRouteConnections(10)
            .withReadTimeout(7500)
            .withConnectTimeout(7500);

    return DockerClientBuilder.getInstance(config)
        .withDockerCmdExecFactory(dockerCmdExecFactory)
        .build();
  }

  public DockerClient create() {
    return createDockerClient(config);
  }
}

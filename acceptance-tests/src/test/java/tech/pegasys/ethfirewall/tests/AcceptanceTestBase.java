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
package tech.pegasys.ethfirewall.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.junit.After;
import org.junit.Before;

public class AcceptanceTestBase {

  private DockerClient dockerClient;
  private String pantheonId;
  private EthFirewallProcessRunner ethFirewallRunner;

  @Before
  public void setUpBase() {
    final DefaultDockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    final DockerCmdExecFactory dockerCmdExecFactory =
        new JerseyDockerCmdExecFactory()
            .withReadTimeout(1000)
            .withConnectTimeout(1000)
            .withMaxTotalConnections(100)
            .withMaxPerRouteConnections(10);

    dockerClient =
        DockerClientBuilder.getInstance(config)
            .withDockerCmdExecFactory(dockerCmdExecFactory)
            .build();

    try {
      final CreateContainerResponse pantheon =
          dockerClient.createContainerCmd("pegasyseng/pantheon:latest").exec();
      pantheonId = pantheon.getId();
      dockerClient.startContainerCmd(pantheonId).exec();
    } catch (final NotFoundException e) {
      throw new RuntimeException(
          "Before you run the acceptance tests, execute 'docker pull pegasyseng/pantheon:latest'",
          e);
    }

    ethFirewallRunner = new EthFirewallProcessRunner();
    ethFirewallRunner.start("EthFirewall");
  }

  @After
  public void tearDownBase() {
    if (dockerClient != null && pantheonId != null) {
      dockerClient.stopContainerCmd(pantheonId).exec();
      dockerClient.waitContainerCmd(pantheonId).exec((new WaitContainerResultCallback()));
      dockerClient.removeContainerCmd(pantheonId).exec();
    }

    ethFirewallRunner.shutdown();
  }

  // TODO ports! need the EthFirewall ports for request / response - or provide functions!
}

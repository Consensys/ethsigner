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
package tech.pegasys.ethsigner.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;

public class AcceptanceTestBase {

  private static final Logger LOG = LogManager.getLogger();

  private DockerClient dockerClient;
  private String pantheonId;
  private EthSignerProcessRunner ethSignerRunner;

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
      // Bind the exposed 8545-8546 ports from the container to 8545-8546 on the host
      final HostConfig portBindingConfig =
          HostConfig.newHostConfig()
              .withPortBindings(PortBinding.parse("8545:8545"), PortBinding.parse("8546:8546"));

      final CreateContainerCmd createPantheon =
          dockerClient
              .createContainerCmd("pegasyseng/pantheon:latest")
              .withHostConfig(portBindingConfig)
              .withCmd(
                  "--miner-enabled",
                  "--miner-coinbase",
                  "fe3b557e8fb62b89f4916b721be55ceb828dbd73",
                  "--rpc-http-cors-origins=\"all\"",
                  "--rpc-http-enabled",
                  "--rpc-ws-enabled",
                  "--network=dev");

      final CreateContainerResponse pantheon = createPantheon.exec();
      pantheonId = pantheon.getId();
      dockerClient.startContainerCmd(pantheonId).exec();
    } catch (final NotFoundException e) {
      throw new RuntimeException(
          "Before you run the acceptance tests, execute 'docker pull pegasyseng/pantheon:latest'",
          e);
    }

    ethSignerRunner = new EthSignerProcessRunner();
    ethSignerRunner.start("EthFirewall");

    try {
      Thread.sleep(2500L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // TODO Pantheon takes over 1 second to startup - problem?

  }

  @After
  public void tearDownBase() {
    if (dockerClient != null && pantheonId != null) {
      LOG.info("Stopping the Pantheon Docker container");
      dockerClient.stopContainerCmd(pantheonId).exec();
      dockerClient.waitContainerCmd(pantheonId).exec((new WaitContainerResultCallback()));
      LOG.info("Removing the Pantheon Docker container");
      dockerClient.removeContainerCmd(pantheonId).exec();
    }

    LOG.info("Shutting down EthSigner");
    ethSignerRunner.shutdown();

    try {
      Thread.sleep(1000L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  // TODO ports! need the EthFirewall ports for request / response - or provide functions!
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

public class AcceptanceTestBase {

  private static final Logger LOG = LogManager.getLogger();
  private static final String LOCALHOST = "127.0.0.1";

  private final Web3j ethSignerJsonRpc;
  private final Web3j ethNodeJsonRpc;

  private EthSignerProcessRunner ethSignerRunner;
  private DockerClient docker;
  private String pantheonContainerId;

  public AcceptanceTestBase() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::tearDownBase));

    ethSignerJsonRpc =
        new JsonRpc2_0Web3j(
            new HttpService("http://" + LOCALHOST + ":" + 9945),
            2000,
            Async.defaultExecutorService());

    ethNodeJsonRpc =
        new JsonRpc2_0Web3j(
            new HttpService("http://" + LOCALHOST + ":" + 8545),
            2000,
            Async.defaultExecutorService());
  }

  @Before
  public void setUpBase() {
    docker = createDockerClient();
    pantheonContainerId = createPantheonContainer();
    startPantheonContainer();

    ethSignerRunner = new EthSignerProcessRunner();
    ethSignerRunner.start("EthSigner");

    awaitPantheonStartup();
    awaitEthSignerStartup();
  }

  @After
  public void tearDownBase() {
    if (hasPantheonContainer()) {
      stopPantheonContainer();
      removePantheonContainer();
    }

    stopEthSigner();
  }

  protected Web3j ethSigner() {
    return ethSignerJsonRpc;
  }

  protected Web3j ethNode() {
    return ethNodeJsonRpc;
  }

  private void awaitEthSignerStartup() {
    LOG.info("Waiting for EthSigner to become responsive...");
    waitFor(() -> assertThat(ethSignerJsonRpc.ethBlockNumber().send().hasError()).isFalse());
    LOG.info("EthSigner is now responsive");
  }

  private void awaitPantheonStartup() {
    LOG.info("Waiting for Pantheon to become responsive...");
    waitFor(() -> assertThat(ethNodeJsonRpc.ethBlockNumber().send().hasError()).isFalse());
    LOG.info("Pantheon is now responsive");
  }

  private DockerClient createDockerClient() {
    final DefaultDockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    final DockerCmdExecFactory dockerCmdExecFactory =
        new JerseyDockerCmdExecFactory()
            .withReadTimeout(1000)
            .withConnectTimeout(1000)
            .withMaxTotalConnections(100)
            .withMaxPerRouteConnections(10);

    return DockerClientBuilder.getInstance(config)
        .withDockerCmdExecFactory(dockerCmdExecFactory)
        .build();
  }

  private String createPantheonContainer() {
    try {
      // Bind the exposed 8545-8546 ports from the container to 8545-8546 on the host
      final HostConfig portBindingConfig =
          HostConfig.newHostConfig()
              .withPortBindings(PortBinding.parse("8545:8545"), PortBinding.parse("8546:8546"));

      final CreateContainerCmd createPantheon =
          docker
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
      return pantheon.getId();
    } catch (final NotFoundException e) {
      throw new RuntimeException(
          "Before you run the acceptance tests, execute 'docker pull pegasyseng/pantheon:latest'",
          e);
    }
  }

  private void startPantheonContainer() {
    docker.startContainerCmd(pantheonContainerId).exec();
  }

  private boolean hasPantheonContainer() {
    return docker != null && pantheonContainerId != null;
  }

  private void stopPantheonContainer() {
    try {
      LOG.info("Stopping the Pantheon Docker container");
      docker.stopContainerCmd(pantheonContainerId).exec();
      docker.waitContainerCmd(pantheonContainerId).exec((new WaitContainerResultCallback()));
    } catch (final NotModifiedException e) {
      LOG.error("Pantheon Docker container has already stopped");
    }
  }

  private void removePantheonContainer() {
    LOG.info("Removing the Pantheon Docker container");
    docker.removeContainerCmd(pantheonContainerId).withForce(true).exec();
  }

  private void stopEthSigner() {
    LOG.info("Shutting down EthSigner");
    ethSignerRunner.shutdown();
  }
}

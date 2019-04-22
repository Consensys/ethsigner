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
package tech.pegasys.ethsigner.tests.dsl.node;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import tech.pegasys.ethsigner.tests.dsl.Accounts;
import tech.pegasys.ethsigner.tests.dsl.Transactions;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

public class PantheonNode implements Node {

  private static final Logger LOG = LogManager.getLogger();
  private static final String PANTHEON_IMAGE = "pegasyseng/pantheon:latest";

  private final DockerClient docker;
  private final String pantheonContainerId;
  private final Transactions transaction;
  private final Accounts accounts;
  private final Web3j jsonRpc;

  public PantheonNode(final DockerClient docker, final NodeConfiguration config) {

    LOG.info("Pantheon Web3j service targeting: " + config.getDownstreamUrl());

    this.jsonRpc =
        new JsonRpc2_0Web3j(
            new HttpService(config.getDownstreamUrl()),
            config.getPollingInterval().toMillis(),
            Async.defaultExecutorService());

    this.docker = docker;
    pullPantheonImage();
    this.pantheonContainerId = createPantheonContainer(config);
    this.transaction = new Transactions(jsonRpc);
    this.accounts = new Accounts(jsonRpc);
  }

  @Override
  public void start() {
    LOG.info("Starting Pantheon Docker container: " + pantheonContainerId);
    docker.startContainerCmd(pantheonContainerId).exec();
  }

  @Override
  public void shutdown() {
    if (hasPantheonContainer()) {
      stopPantheonContainer();
      removePantheonContainer();
    }
  }

  @Override
  public void awaitStartupCompletion() {
    LOG.info("Waiting for Pantheon to become responsive...");
    waitFor(() -> assertThat(jsonRpc.ethBlockNumber().send().hasError()).isFalse());
    LOG.info("Pantheon is now responsive");
  }

  @Override
  public Accounts accounts() {
    return accounts;
  }

  @Override
  public Transactions transactions() {
    return transaction;
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

  private void pullPantheonImage() {
    final PullImageResultCallback callback = new PullImageResultCallback();
    docker.pullImageCmd(PANTHEON_IMAGE).exec(callback);

    try {
      LOG.info("Pulling the Pantheon Docker image...");
      callback.awaitCompletion();
      LOG.info("Pulled the Pantheon Docker image: " + PANTHEON_IMAGE);
    } catch (final InterruptedException e) {
      LOG.error(e);
    }
  }

  private String createPantheonContainer(final NodeConfiguration config) {
    final HostConfig portBindingConfig =
        HostConfig.newHostConfig().withPortBindings(tcpPortBinding(config), wsPortBinding(config));

    try {
      final CreateContainerCmd createPantheon =
          docker
              .createContainerCmd(PANTHEON_IMAGE)
              .withHostConfig(portBindingConfig)
              .withCmd(
                  "--miner-enabled",
                  "--miner-coinbase",
                  "fe3b557e8fb62b89f4916b721be55ceb828dbd73",
                  "--rpc-http-cors-origins",
                  "all",
                  "--host-whitelist",
                  "*",
                  "--rpc-http-enabled",
                  "--rpc-ws-enabled",
                  "--network=dev");

      LOG.info("Creating the Pantheon Docker image...");
      final CreateContainerResponse pantheon = createPantheon.exec();
      LOG.info("Created Pantheon Docker image, id: " + pantheon.getId());
      return pantheon.getId();
    } catch (final NotFoundException e) {
      throw new RuntimeException(
          "Before you run the acceptance tests, execute 'docker pull pegasyseng/pantheon:latest'",
          e);
    }
  }

  private PortBinding tcpPortBinding(final NodeConfiguration config) {
    return PortBinding.parse(config.getTcpPort() + ":8545");
  }

  private PortBinding wsPortBinding(final NodeConfiguration config) {
    return PortBinding.parse(config.getWsPort() + ":8546");
  }
}

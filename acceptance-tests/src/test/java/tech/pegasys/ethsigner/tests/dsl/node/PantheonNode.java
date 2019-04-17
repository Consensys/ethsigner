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

import java.io.Closeable;
import java.io.IOException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

public class PantheonNode implements Node {

  private static final Logger LOG = LogManager.getLogger();

  private final DockerClient docker;
  private final String pantheonContainerId;
  private final Web3j jsonRpc;

  public PantheonNode(final DockerClient docker, final NodeConfiguration config) {
    this.jsonRpc =
        new JsonRpc2_0Web3j(
            new HttpService(config.downstreamUrl()),
            config.pollingInterval().toMillis(),
            Async.defaultExecutorService());

    this.docker = docker;
    this.pantheonContainerId = createPantheonContainer(config);
  }

  @Override
  public Web3j web3j() {
    return jsonRpc;
  }

  @Override
  public void start() {
    docker.startContainerCmd(pantheonContainerId).exec();
  }

  @Override
  public void shutdown() {
    if (hasPantheonContainer()) {
      stopPantheonContainer();
      removePantheonContainer();
    }
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

  private static class CallbackThing implements ResultCallback<PullResponseItem> {

    @Override
    public void onStart(Closeable closeable) {}

    @Override
    public void onNext(PullResponseItem object) {}

    @Override
    public void onError(Throwable throwable) {}

    @Override
    public void onComplete() {}

    @Override
    public void close() throws IOException {}
  }

  private String createPantheonContainer(final NodeConfiguration config) {
    final HostConfig portBindingConfig =
        HostConfig.newHostConfig().withPortBindings(tcpPortBinding(config), wsPortBinding(config));

    try {
      // TODO pull the image, avoid permissioning  problems locally
      docker.pullImageCmd("pegasyseng/pantheon:latest").exec(new CallbackThing());

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

  private PortBinding tcpPortBinding(final NodeConfiguration config) {
    return PortBinding.parse(config.tcpPort() + ":8545");
  }

  private PortBinding wsPortBinding(final NodeConfiguration config) {
    return PortBinding.parse(config.wsPort() + ":8546");
  }
}

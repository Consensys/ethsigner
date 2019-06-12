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
package tech.pegasys.ethsigner.tests.hashicorpVault;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HashicorpVaultDocker {

  private static final Logger LOG = LogManager.getLogger();
  private static final String HASHICORP_VAULT_IMAGE = "vault:latest";
  private static final int DEFAULT_HTTP_PORT = 8200;
  public static final String vaultToken = "token";
  private static final String[] CREATE_ETHSIGNER_SIGNING_KEY_SECRET = {
    "vault",
    "kv",
    "put",
    "secret/ethsignerSigningKey",
    "value=8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63"
  };
  private static final String[] COMMAND_TO_CHECK_VAULT_IS_UP = {"vault", "status"};
  private static final String LOCALHOST = "localhost";
  private static final String EXPECTED_FOR_SECRET_CREATION = "created_time";
  private static final String EXPECTED_FOR_STATUS = "Sealed";

  private final DockerClient docker;
  private final String vaultContainerId;

  private int port;
  private String ipAddress;

  public HashicorpVaultDocker(final DockerClient docker) {
    this.docker = docker;
    pullVaultImage();
    this.vaultContainerId = createVaultContainer();
  }

  public void start() {
    LOG.info("Starting Hashicorp Vault Docker container: {}", vaultContainerId);
    docker.startContainerCmd(vaultContainerId).exec();

    this.ipAddress = getDockerHostIp();
    LOG.info("Docker Host IP address: {}", ipAddress);

    LOG.info("Querying for the Docker dynamically allocated vault port number");
    final InspectContainerResponse containerResponse =
        docker.inspectContainerCmd(vaultContainerId).exec();

    final Ports ports = containerResponse.getNetworkSettings().getPorts();
    port = httpRpcPort(ports);
    LOG.info("Http port for Hashicorp Vault: {}", port);
  }

  public void createTestData() {
    LOG.info("creating the secret in vault that contains the private key.");
    final ExecCreateCmdResponse execCreateCmdResponse =
        getExecCreateCmdResponse(CREATE_ETHSIGNER_SIGNING_KEY_SECRET);
    waitFor(
        60,
        () ->
            assertThat(
                    runCommandInVaultContainer(execCreateCmdResponse, EXPECTED_FOR_SECRET_CREATION))
                .isTrue());
    LOG.info("The secret was created successfully.");
  }

  private String getDockerHostIp() {
    final DefaultDockerClientConfig dockerConfig =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    final Optional<String> optional = Optional.of(dockerConfig.getDockerHost()).map(URI::getHost);
    return optional.orElse(LOCALHOST);
  }

  private ExecCreateCmdResponse getExecCreateCmdResponse(final String[] commandWithArguments) {
    return docker
        .execCreateCmd(vaultContainerId)
        .withAttachStdout(true)
        .withAttachStderr(true)
        .withCmd(commandWithArguments)
        .exec();
  }

  private boolean runCommandInVaultContainer(
      final ExecCreateCmdResponse execCreateCmdResponse, final String expectedInStdout)
      throws InterruptedException {
    final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    final ExecStartResultCallback resultCallback = new ExecStartResultCallback(stdout, stderr);
    LOG.info(
        "execCreateCmdResponse with id: {}, containerId: {}",
        execCreateCmdResponse.getId(),
        vaultContainerId);
    final ExecStartResultCallback execStartResultCallback =
        docker.execStartCmd(execCreateCmdResponse.getId()).exec(resultCallback).awaitCompletion();
    execStartResultCallback.onError(
        new RuntimeException(
            "command in Hashicorp Vault returned error\n" + execStartResultCallback.toString()));
    final String stdoutString = stdout.toString();
    return stdoutString.contains(expectedInStdout);
  }

  public void shutdown() {
    if (hasVaultContainer()) {
      stopVaultContainer();
      removeVaultContainer();
    }
  }

  public void awaitStartupCompletion() {
    LOG.info("Waiting for Hashicorp Vault to become responsive...");
    final ExecCreateCmdResponse execCreateCmdResponse =
        getExecCreateCmdResponse(COMMAND_TO_CHECK_VAULT_IS_UP);
    waitFor(
        60,
        () ->
            assertThat(runCommandInVaultContainer(execCreateCmdResponse, EXPECTED_FOR_STATUS))
                .isTrue());
    LOG.info("Hashicorp Vault is now responsive");
  }

  public int port() {
    return port;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  private boolean hasVaultContainer() {
    return docker != null && vaultContainerId != null;
  }

  private void stopVaultContainer() {
    try {
      LOG.info("Stopping the Vault Docker container...");
      docker.stopContainerCmd(vaultContainerId).exec();
      final WaitContainerResultCallback waiter = new WaitContainerResultCallback();
      docker.waitContainerCmd(vaultContainerId).exec((waiter));
      waiter.awaitCompletion();
      LOG.info("Stopped the Vault Docker container");
    } catch (final NotModifiedException e) {
      LOG.error("Vault Docker container has already stopped");
    } catch (final InterruptedException e) {
      LOG.error("Interrupted when waiting for Vault Docker container to stop");
    }
  }

  private void removeVaultContainer() {
    LOG.info("Removing the Vault Docker container...");
    docker.removeContainerCmd(vaultContainerId).withForce(true).exec();
    LOG.info("Removed the Vault Docker container");
  }

  private void pullVaultImage() {
    final PullImageResultCallback callback = new PullImageResultCallback();
    docker.pullImageCmd(HASHICORP_VAULT_IMAGE).exec(callback);

    try {
      LOG.info("Pulling the Vault Docker image...");
      callback.awaitCompletion();
      LOG.info("Pulled the Vault Docker image: " + HASHICORP_VAULT_IMAGE);
    } catch (final InterruptedException e) {
      LOG.error(e);
    }
  }

  private String createVaultContainer() {
    final HostConfig hostConfig =
        HostConfig.newHostConfig()
            .withPortBindings(httpPortBinding())
            .withCapAdd(Capability.IPC_LOCK);

    final List<String> enviromentVars = new ArrayList<>();
    enviromentVars.add("VAULT_DEV_ROOT_TOKEN_ID=token");
    enviromentVars.add("VAULT_ADDR=http://127.0.0.1:8200");
    enviromentVars.add("VAULT_TOKEN=token");

    try {
      final CreateContainerCmd createVault =
          docker
              .createContainerCmd(HASHICORP_VAULT_IMAGE)
              .withHostConfig(hostConfig)
              .withEnv(enviromentVars);

      LOG.info("Creating the Vault Docker container...");
      final CreateContainerResponse vault = createVault.exec();
      LOG.info("Created Vault Docker container, id: " + vault.getId());
      return vault.getId();
    } catch (final NotFoundException e) {
      throw new RuntimeException(
          "Before you run the acceptance tests, execute 'docker pull vault:latest'", e);
    }
  }

  private PortBinding httpPortBinding() {
    return new PortBinding(new Binding(null, null), ExposedPort.tcp(DEFAULT_HTTP_PORT));
  }

  private int httpRpcPort(final Ports ports) {
    return portSpec(ports, DEFAULT_HTTP_PORT);
  }

  private int portSpec(final Ports ports, final int exposedPort) {
    final Binding[] tcpPorts = ports.getBindings().get(ExposedPort.tcp(exposedPort));
    assertThat(tcpPorts).isNotEmpty();
    assertThat(tcpPorts.length).isEqualTo(1);

    return Integer.parseInt(tcpPorts[0].getHostPortSpec());
  }
}

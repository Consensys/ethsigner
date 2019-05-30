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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

  private final DockerClient docker;
  private final String vaultContainerId;

  private int port;

  public HashicorpVaultDocker(final DockerClient docker) {
    this.docker = docker;
    pullVaultImage();
    this.vaultContainerId = createVaultContainer();
  }

  public void start() {
    LOG.info("Starting Hashicorp Vault Docker container: {}", vaultContainerId);
    docker.startContainerCmd(vaultContainerId).exec();

    LOG.info("Querying for the Docker dynamically allocated vault port number");
    final InspectContainerResponse containerResponse =
        docker.inspectContainerCmd(vaultContainerId).exec();
    final Ports ports = containerResponse.getNetworkSettings().getPorts();
    port = httpRpcPort(ports);
    LOG.info("Http port for Hashicorp Vault: {}", port);

    try {
      TimeUnit.SECONDS.sleep(10); // wait until the docker is container is all up and running.
    } catch (InterruptedException e) {
      LOG.error("Interruption while waiting for Hashicorp Vault to be up");
    }

    // After starting the docker container with the vault we need to create the secret that contains
    // the
    // the private key. That is done in the awaitStartupCompletion, because we need to wait until a
    // call to
    // the vault succeeds to know that it is up. That call now creates the secret.
  }

  private ExecCreateCmdResponse getExecCreateCmdResponse(String[] commandWithArguments) {
    return docker
        .execCreateCmd(vaultContainerId)
        .withAttachStdout(true)
        .withAttachStderr(true)
        .withCmd(commandWithArguments)
        .exec();
  }

  private boolean runCommandInVaultContainer(
      ExecCreateCmdResponse execCreateCmdResponse, String expectedInStdout)
      throws InterruptedException {
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    final ExecStartResultCallback resultCallback = new ExecStartResultCallback(stdout, stderr);
    final ExecStartResultCallback execStartResultCallback =
        docker.execStartCmd(execCreateCmdResponse.getId()).exec(resultCallback).awaitCompletion();
    execStartResultCallback.onError(
        new RuntimeException(
            "command in Hashicorp Vault returned error\n" + execStartResultCallback.toString()));
    if (expectedInStdout != null) {
      final String stdoutString = stdout.toString();
      return stdoutString.indexOf(expectedInStdout) != -1;
    } else {
      return true;
    }
  }

  public void shutdown() {
    if (hasVaultContainer()) {
      stopVaultContainer();
      removeVaultContainer();
    }
  }

  /** This method now also creates the secret that contains the private signing key. */
  public void awaitStartupCompletion() {
    LOG.info("Waiting for Hashicorp Vault to become responsive...");
    final ExecCreateCmdResponse execCreateCmdResponse =
        getExecCreateCmdResponse(CREATE_ETHSIGNER_SIGNING_KEY_SECRET);
    waitFor(
        () ->
            assertThat(runCommandInVaultContainer(execCreateCmdResponse, "created_time")).isTrue());
    LOG.info("Hashicorp Vault is now responsive");
  }

  public int port() {
    return port;
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

    final List<String> enviromentVars = new ArrayList<String>();
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

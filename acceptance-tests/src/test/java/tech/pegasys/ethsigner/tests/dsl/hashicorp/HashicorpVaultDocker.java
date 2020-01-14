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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;

public class HashicorpVaultDocker {

  private static final Logger LOG = LogManager.getLogger();
  private static final String HASHICORP_VAULT_IMAGE = "vault:1.2.3";
  private static final String DEFAULT_VAULT_HOST = "localhost";
  private static final int DEFAULT_VAULT_PORT = 8200;
  // hashicorp kv-v2 /secret/key is accessible from path /v1/secret/data/key.
  // EthSigner (Hashicorp Signer) expects user to provide /secret/data/key and it appends /v1
  private static final String SECRET_PATH = "/secret";
  private static final String SIGNING_KEY_RESOURCE = "/ethsignerSigningKey";
  private static final String VAULT_PUT_RESOURCE = SECRET_PATH + SIGNING_KEY_RESOURCE;
  public static final String VAULT_SIGNING_KEY_GET_RESOURCE =
      SECRET_PATH + "/data" + SIGNING_KEY_RESOURCE;

  private static final String EXPECTED_FOR_SECRET_CREATION = "created_time";
  private static final String EXPECTED_FOR_STATUS = "Sealed";
  private static final Path CONTAINER_MOUNT_PATH = Path.of("/vault/config");

  private final DockerClient docker;
  private final HashicorpVaultCerts hashicorpVaultCerts;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private String vaultContainerId;
  private int port;
  private String ipAddress;
  private String hashicorpRootToken;

  private HashicorpVaultDocker(
      final DockerClient docker, final HashicorpVaultCerts hashicorpVaultCerts) {
    this.docker = docker;
    this.hashicorpVaultCerts = hashicorpVaultCerts;
  }

  public static HashicorpVaultDocker createVaultDocker(
      final DockerClient docker, final HashicorpVaultCerts hashicorpVaultCerts) {
    final HashicorpVaultDocker hashicorpVaultDocker =
        new HashicorpVaultDocker(docker, hashicorpVaultCerts);
    hashicorpVaultDocker.pullVaultImage();
    hashicorpVaultDocker.createVaultContainer();
    hashicorpVaultDocker.start();
    hashicorpVaultDocker.awaitStartupCompletion();
    hashicorpVaultDocker.postStartup();
    hashicorpVaultDocker.createSecretKey();
    return hashicorpVaultDocker;
  }

  public void shutdown() {
    if (docker != null && vaultContainerId != null) {
      stopVaultContainer();
      removeVaultContainer();
    }
  }

  public int getPort() {
    return port;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public boolean isTlsEnabled() {
    return hashicorpVaultCerts != null;
  }

  public String getHashicorpRootToken() {
    return hashicorpRootToken;
  }

  private void start() {
    LOG.info("Starting Hashicorp Vault Docker container: {}", vaultContainerId);
    docker.startContainerCmd(vaultContainerId).exec();

    ipAddress = getDockerHostIp();
    LOG.info("Docker Host IP address: {}", ipAddress);

    LOG.info("Querying for the Docker dynamically allocated vault port number");
    final InspectContainerResponse containerResponse =
        docker.inspectContainerCmd(vaultContainerId).exec();

    final Ports ports = containerResponse.getNetworkSettings().getPorts();
    port = portSpec(ports);
    LOG.info("Http port for Hashicorp Vault: {}", port);
  }

  private void awaitStartupCompletion() {
    LOG.info("Waiting for Hashicorp Vault to become responsive...");

    waitFor(
        60,
        () -> {
          final ExecCreateCmdResponse execCreateCmdResponse =
              getExecCreateCmdResponse(vaultStatusCommand());
          assertThat(
                  runCommandInVaultContainerAndCompareOutput(
                      execCreateCmdResponse, EXPECTED_FOR_STATUS))
              .isTrue();
        });
    LOG.info("Hashicorp Vault is now responsive");
  }

  private void postStartup() {
    final HashicorpVaultTokens hashicorpVaultTokens = initVault();
    unseal(hashicorpVaultTokens.getUnsealKey());
    login(hashicorpVaultTokens.getRootToken());
    enableHashicorpKeyValueV2Engine();
    hashicorpRootToken = hashicorpVaultTokens.getRootToken();
  }

  private void createSecretKey() {
    LOG.info("creating the secret in vault that contains the private key.");

    waitFor(
        10,
        () -> {
          final ExecCreateCmdResponse execCreateCmdResponse =
              getExecCreateCmdResponse(vaultPutSecretCommand());
          assertThat(
                  runCommandInVaultContainerAndCompareOutput(
                      execCreateCmdResponse, EXPECTED_FOR_SECRET_CREATION))
              .isTrue();
        });
    LOG.info("The secret was created successfully.");
  }

  private HashicorpVaultTokens initVault() {
    LOG.info("Initializing Hashicorp vault ...");
    final ExecCreateCmdResponse execCreateCmdResponse =
        getExecCreateCmdResponse(vaultInitCommand());
    final String jsonOutput =
        Awaitility.await()
            .atMost(10, SECONDS)
            .ignoreExceptions()
            .until(
                () -> runCommandInContainerAndGetOutput(execCreateCmdResponse),
                containsString("root_token"));

    final String rootToken;
    final String unsealKey;
    try {
      final JsonNode jsonNode = objectMapper.readTree(jsonOutput);
      rootToken = jsonNode.get("root_token").asText();
      final JsonNode unsealKeyJsonNodeArray = jsonNode.get("unseal_keys_b64");
      assertThat(unsealKeyJsonNodeArray.isArray()).isTrue();
      unsealKey = unsealKeyJsonNodeArray.get(0).asText();
    } catch (IOException e) {
      throw new RuntimeException("Error in parsing json output from vault unseal command", e);
    }
    assertThat(rootToken).isNotNull();
    assertThat(unsealKey).isNotNull();
    LOG.info("Hashicorp vault is initialized");

    return new HashicorpVaultTokens(unsealKey, rootToken);
  }

  private void unseal(final String unsealKey) {
    LOG.info("Unseal Hashicorp vault ...");
    final ExecCreateCmdResponse execCreateCmdResponse =
        getExecCreateCmdResponse(
            "vault",
            "operator",
            "unseal",
            "-address=" + vaultDefaultUrl(),
            "-format=json",
            unsealKey);
    final String jsonOutput =
        Awaitility.await()
            .atMost(10, SECONDS)
            .ignoreExceptions()
            .until(
                () -> runCommandInContainerAndGetOutput(execCreateCmdResponse),
                containsString("sealed"));

    try {
      final JsonNode jsonNode = objectMapper.readTree(jsonOutput);
      assertThat(jsonNode.get("sealed").asBoolean()).isFalse();
    } catch (IOException e) {
      throw new RuntimeException("Error in parsing json output from vault unseal command", e);
    }

    LOG.info("Hashicorp vault is unsealed");
  }

  private void login(final String rootToken) {
    LOG.info("Login Hashicorp vault CLI ...");
    final ExecCreateCmdResponse execCreateCmdResponse =
        getExecCreateCmdResponse("vault", "login", "-address=" + vaultDefaultUrl(), rootToken);

    Awaitility.await()
        .atMost(10, SECONDS)
        .ignoreExceptions()
        .until(
            () -> {
              final String output = runCommandInContainerAndGetOutput(execCreateCmdResponse);
              return output.contains(rootToken);
            });

    LOG.info("Hashicorp vault CLI login successful");
  }

  private void enableHashicorpKeyValueV2Engine() {
    LOG.info("Mounting /secret kv-v2 in Hashicorp vault ...");
    final ExecCreateCmdResponse execCreateCmdResponse =
        getExecCreateCmdResponse(vaultEnableSecretEngineCommand());

    Awaitility.await()
        .atMost(10, SECONDS)
        .ignoreExceptions()
        .until(
            () -> {
              final String output = runCommandInContainerAndGetOutput(execCreateCmdResponse);
              return output.contains("Success");
            });

    LOG.info("Hashicorp vault kv-v2 /secret is mounted");
  }

  private String vaultDefaultUrl() {
    return String.format(
        "%s://%s:%d", isTlsEnabled() ? "https" : "http", DEFAULT_VAULT_HOST, DEFAULT_VAULT_PORT);
  }

  private String[] vaultStatusCommand() {
    return new String[] {"vault", "status", "-address=" + vaultDefaultUrl()};
  }

  private String[] vaultInitCommand() {
    return new String[] {
      "vault",
      "operator",
      "init",
      "-key-shares=1",
      "-key-threshold=1",
      "-format=json",
      "-address=" + vaultDefaultUrl()
    };
  }

  private String[] vaultEnableSecretEngineCommand() {
    return new String[] {
      "vault",
      "secrets",
      "enable",
      "-address=" + vaultDefaultUrl(),
      "-path=" + SECRET_PATH,
      "kv-v2",
    };
  }

  private String[] vaultPutSecretCommand() {
    return new String[] {
      "vault",
      "kv",
      "put",
      "-address=" + vaultDefaultUrl(),
      VAULT_PUT_RESOURCE,
      "value=8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
    };
  }

  private String getDockerHostIp() {
    final DefaultDockerClientConfig dockerConfig =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    return Optional.of(dockerConfig.getDockerHost()).map(URI::getHost).orElse(DEFAULT_VAULT_HOST);
  }

  private ExecCreateCmdResponse getExecCreateCmdResponse(final String... commandWithArguments) {
    return docker
        .execCreateCmd(vaultContainerId)
        .withAttachStdout(true)
        .withAttachStderr(true)
        .withCmd(commandWithArguments)
        .exec();
  }

  private boolean runCommandInVaultContainerAndCompareOutput(
      final ExecCreateCmdResponse execCreateCmdResponse, final String expectedInStdout)
      throws InterruptedException {
    return runCommandInContainerAndGetOutput(execCreateCmdResponse).contains(expectedInStdout);
  }

  private String runCommandInContainerAndGetOutput(
      final ExecCreateCmdResponse execCreateCmdResponse) throws InterruptedException {
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
    return stdout.toString();
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

  private void createVaultContainer() {
    final Volume configVolume = new Volume(CONTAINER_MOUNT_PATH.toString());

    final HostConfig hostConfig =
        HostConfig.newHostConfig()
            .withPortBindings(httpPortBinding())
            .withCapAdd(Capability.IPC_LOCK);

    if (isTlsEnabled()) {
      final Bind configBind =
          new Bind(hashicorpVaultCerts.getCertificateDirectory().toString(), configVolume);
      hostConfig.withBinds(configBind);
    }

    final List<String> environmentVariables =
        List.of(
            "VAULT_LOCAL_CONFIG={\"storage\": {\"inmem\":{}}, "
                + "\"default_lease_ttl\": \"168h\", \"max_lease_ttl\": \"720h\", "
                + "\"listener\": {\"tcp\": {"
                + tcpEnvAddressConfig()
                + tlsEnvConfig()
                + "}}}",
            "VAULT_SKIP_VERIFY=true");

    try {
      final CreateContainerCmd createVault =
          docker
              .createContainerCmd(HASHICORP_VAULT_IMAGE)
              .withHostConfig(hostConfig)
              .withEnv(environmentVariables)
              .withCmd("server");

      if (isTlsEnabled()) {
        createVault.withVolumes(configVolume);
      }

      LOG.info("Creating the Vault Docker container (TLS={})...", isTlsEnabled());
      final CreateContainerResponse vault = createVault.exec();
      LOG.info("Created Vault Docker container, id: " + vault.getId());
      this.vaultContainerId = vault.getId();
    } catch (final NotFoundException e) {
      throw new RuntimeException(
          HASHICORP_VAULT_IMAGE + " image has been removed after initial pull.", e);
    }
  }

  private String tcpEnvAddressConfig() {
    return String.format("\"address\":\"0.0.0.0:%d\"", DEFAULT_VAULT_PORT);
  }

  private String tlsEnvConfig() {
    if (!isTlsEnabled()) {
      return ", \"tls_disable\":\"true\"";
    }
    final Path containerTlsCertPath =
        CONTAINER_MOUNT_PATH.resolve(hashicorpVaultCerts.getTlsCertificate().getFileName());
    final Path containerTlsKeyPath =
        CONTAINER_MOUNT_PATH.resolve(hashicorpVaultCerts.getTlsPrivateKey().getFileName());

    return String.format(
        ", \"tls_min_version\": \"tls12\", \"tls_cert_file\": \"%s\", \"tls_key_file\": \"%s\"",
        containerTlsCertPath.toString(), containerTlsKeyPath.toString());
  }

  private PortBinding httpPortBinding() {
    return new PortBinding(new Binding(null, null), ExposedPort.tcp(DEFAULT_VAULT_PORT));
  }

  private int portSpec(final Ports ports) {
    final Binding[] tcpPorts = ports.getBindings().get(ExposedPort.tcp(DEFAULT_VAULT_PORT));
    assertThat(tcpPorts).isNotEmpty();
    assertThat(tcpPorts.length).isEqualTo(1);

    return Integer.parseInt(tcpPorts[0].getHostPortSpec());
  }

  private void waitFor(final int timeoutSeconds, final ThrowingRunnable condition) {
    Awaitility.await()
        .ignoreExceptions()
        .atMost(timeoutSeconds, TimeUnit.SECONDS)
        .untilAsserted(condition);
  }
}

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
import tech.pegasys.ethsigner.tests.dsl.Eea;
import tech.pegasys.ethsigner.tests.dsl.Eth;
import tech.pegasys.ethsigner.tests.dsl.PrivateContracts;
import tech.pegasys.ethsigner.tests.dsl.PublicContracts;
import tech.pegasys.ethsigner.tests.dsl.RawJsonRpcRequestFactory;
import tech.pegasys.ethsigner.tests.dsl.Transactions;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.core.ConditionTimeoutException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.eea.JsonRpc2_0Eea;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

public class PantheonNode implements Node {

  private static final Logger LOG = LogManager.getLogger();

  private static final String PANTHEON_IMAGE = "pegasyseng/pantheon:latest";
  private static final String HTTP_URL_FORMAT = "http://%s:%s";

  /** Pantheon's dev.json has the hard fork at block 0 */
  private static final BigInteger SPURIOUS_DRAGON_HARD_FORK_BLOCK = BigInteger.valueOf(1);

  private static final int DEFAULT_HTTP_RPC_PORT = 8545;
  private static final int DEFAULT_WS_RPC_PORT = 8546;

  private final DockerClient docker;
  private final String pantheonContainerId;
  private final long pollingInterval;
  private final String hostname;

  private Accounts accounts;
  private Transactions transactions;
  private Web3j jsonRpc;
  private NodePorts ports;
  private PublicContracts publicContracts;
  private PrivateContracts privateContracts;

  public PantheonNode(final DockerClient docker, final NodeConfiguration config) {
    this.docker = docker;
    pullPantheonImage();
    this.pantheonContainerId = createPantheonContainer(config);
    this.pollingInterval = config.getPollingInterval().toMillis();
    this.hostname = config.getHostname();
  }

  @Override
  public void start() {
    LOG.info("Starting Pantheon Docker container: {}", pantheonContainerId);
    docker.startContainerCmd(pantheonContainerId).exec();

    LOG.info("Querying for the Docker dynamically allocated RPC port numbers");
    final InspectContainerResponse containerResponse =
        docker.inspectContainerCmd(pantheonContainerId).exec();
    final Ports ports = containerResponse.getNetworkSettings().getPorts();
    final int httpRpcPort = httpRpcPort(ports);
    final int wsRpcPort = wsRpcPort(ports);
    LOG.info("Http RPC port: {}, Web Socket RPC port: {}", httpRpcPort, wsRpcPort);

    final String httpRpcUrl = url(httpRpcPort);
    LOG.info("Pantheon Web3j service targeting: {} ", httpRpcUrl);

    final HttpService web3jHttpService = new HttpService(httpRpcUrl);
    this.jsonRpc =
        new JsonRpc2_0Web3j(web3jHttpService, pollingInterval, Async.defaultExecutorService());
    final RawJsonRpcRequestFactory requestFactory = new RawJsonRpcRequestFactory(web3jHttpService);
    final JsonRpc2_0Eea eeaJsonRpc = new JsonRpc2_0Eea(web3jHttpService);
    final Eth eth = new Eth(jsonRpc);
    final Eea eea = new Eea(eeaJsonRpc, requestFactory);
    this.accounts = new Accounts(eth);
    this.publicContracts = new PublicContracts(eth);
    this.privateContracts = new PrivateContracts(eea);
    this.transactions = new Transactions(eth);
    this.ports = new NodePorts(httpRpcPort, wsRpcPort);
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
    try {
      LOG.info("Waiting for Pantheon to become responsive...");
      waitFor(() -> assertThat(jsonRpc.ethBlockNumber().send().hasError()).isFalse());
      LOG.info("Pantheon is now responsive");
      waitFor(
          () ->
              assertThat(jsonRpc.ethBlockNumber().send().getBlockNumber())
                  .isGreaterThan(SPURIOUS_DRAGON_HARD_FORK_BLOCK));
    } catch (final ConditionTimeoutException e) {
      showLogFromPantheonContainer();
      throw new RuntimeException("Failed to start the Pantheon node", e);
    }
  }

  private void showLogFromPantheonContainer() {
    docker
        .logContainerCmd(pantheonContainerId)
        .withStdOut(true)
        .withStdErr(true)
        .withFollowStream(true)
        .withTail(500)
        .exec(
            new LogContainerResultCallback() {
              @Override
              public void onNext(Frame item) {
                LOG.info(item.toString());
              }
            });
  }

  @Override
  public NodePorts ports() {
    return ports;
  }

  @Override
  public Accounts accounts() {
    return accounts;
  }

  @Override
  public PublicContracts publicContracts() {
    return publicContracts;
  }

  @Override
  public PrivateContracts privateContracts() {
    return privateContracts;
  }

  @Override
  public Transactions transactions() {
    return transactions;
  }

  private String url(final int port) {
    return String.format(HTTP_URL_FORMAT, hostname, port);
  }

  private boolean hasPantheonContainer() {
    return docker != null && pantheonContainerId != null;
  }

  private void stopPantheonContainer() {
    try {
      LOG.info("Stopping the Pantheon Docker container...");
      docker.stopContainerCmd(pantheonContainerId).exec();
      final WaitContainerResultCallback waiter = new WaitContainerResultCallback();
      docker.waitContainerCmd(pantheonContainerId).exec((waiter));
      waiter.awaitCompletion();
      LOG.info("Stopped the Pantheon Docker container");
    } catch (final NotModifiedException e) {
      LOG.error("Pantheon Docker container has already stopped");
    } catch (final InterruptedException e) {
      LOG.error("Interrupted when waiting for Pantheon Docker container to stop");
    }
  }

  private void removePantheonContainer() {
    LOG.info("Removing the Pantheon Docker container...");
    docker.removeContainerCmd(pantheonContainerId).withForce(true).exec();
    LOG.info("Removed the Pantheon Docker container");
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
    final String genesisFilePath = genesisFilePath(config.getGenesisFilePath());
    LOG.info("Path to Genesis file: {}", genesisFilePath);
    final Volume genesisVolume = new Volume("/etc/pantheon/genesis.json");
    final Bind genesisBinding = new Bind(genesisFilePath, genesisVolume);
    final Bind privacyBinding = privacyVolumeBinding("enclave_key.pub");
    final List<Bind> bindings = Lists.newArrayList(genesisBinding, privacyBinding);

    try {
      final List<String> commandLineItems =
          Lists.newArrayList(
              "--logging",
              "DEBUG",
              "--miner-enabled",
              "--miner-coinbase",
              "1b23ba34ca45bb56aa67bc78be89ac00ca00da00",
              "--host-whitelist",
              "*",
              "--rpc-http-enabled",
              "--rpc-ws-enabled",
              "--rpc-http-apis",
              "ETH,NET,WEB3,EEA",
              "--privacy-enabled");

      config
          .getCors()
          .ifPresent(
              cors -> commandLineItems.addAll(Lists.newArrayList("--rpc-http-cors-origins", cors)));

      LOG.debug("pantheon command line {}", config);

      final HostConfig hostConfig =
          HostConfig.newHostConfig()
              .withPortBindings(httpRpcPortBinding(), wsRpcPortBinding())
              .withBinds(bindings);
      final CreateContainerCmd createPantheon =
          docker
              .createContainerCmd(PANTHEON_IMAGE)
              .withHostConfig(hostConfig)
              .withVolumes(genesisVolume)
              .withCmd(commandLineItems);

      LOG.info("Creating the Pantheon Docker container...");
      final CreateContainerResponse pantheon = createPantheon.exec();
      LOG.info("Created Pantheon Docker container, id: " + pantheon.getId());
      return pantheon.getId();
    } catch (final NotFoundException e) {
      throw new RuntimeException(
          "Before you run the acceptance tests, execute 'docker pull pegasyseng/pantheon:latest'",
          e);
    }
  }

  private String genesisFilePath(final String filename) {
    final URL resource = PantheonNode.class.getResource(filename);
    return resourceFileName(resource);
  }

  @SuppressWarnings("UnstableApiUsage")
  private String privacyPublicKeyFilePath(final String filename) {
    final URL resource = Resources.getResource(filename);
    return resourceFileName(resource);
  }

  private String resourceFileName(final URL resource) {
    try {
      return URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8.name());
    } catch (final UnsupportedEncodingException ex) {
      LOG.error("Unsupported encoding used to decode {}, filepath.", resource);
      throw new RuntimeException("Illegal string decoding");
    }
  }

  private Bind privacyVolumeBinding(final String privacyPublicKey) {
    final String privacyPublicKeyFile = privacyPublicKeyFilePath(privacyPublicKey);
    final Volume privacyPublicKeyVolume = new Volume("/etc/pantheon/privacy_public_key");
    return new Bind(privacyPublicKeyFile, privacyPublicKeyVolume);
  }

  private PortBinding httpRpcPortBinding() {
    return new PortBinding(new Binding(null, null), ExposedPort.tcp(DEFAULT_HTTP_RPC_PORT));
  }

  private PortBinding wsRpcPortBinding() {
    return new PortBinding(new Binding(null, null), ExposedPort.tcp(DEFAULT_WS_RPC_PORT));
  }

  private int wsRpcPort(final Ports ports) {
    return portSpec(ports, DEFAULT_WS_RPC_PORT);
  }

  private int httpRpcPort(final Ports ports) {
    return portSpec(ports, DEFAULT_HTTP_RPC_PORT);
  }

  private int portSpec(final Ports ports, final int exposedPort) {
    final Binding[] tcpPorts = ports.getBindings().get(ExposedPort.tcp(exposedPort));
    assertThat(tcpPorts).isNotEmpty();
    assertThat(tcpPorts.length).isEqualTo(1);

    return Integer.parseInt(tcpPorts[0].getHostPortSpec());
  }
}

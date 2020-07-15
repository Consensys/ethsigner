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
package tech.pegasys.ethsigner.tests.dsl.node.besu;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import tech.pegasys.ethsigner.tests.dsl.Accounts;
import tech.pegasys.ethsigner.tests.dsl.Besu;
import tech.pegasys.ethsigner.tests.dsl.Eea;
import tech.pegasys.ethsigner.tests.dsl.Eth;
import tech.pegasys.ethsigner.tests.dsl.PrivateContracts;
import tech.pegasys.ethsigner.tests.dsl.PublicContracts;
import tech.pegasys.ethsigner.tests.dsl.RawJsonRpcRequestFactory;
import tech.pegasys.ethsigner.tests.dsl.Transactions;
import tech.pegasys.ethsigner.tests.dsl.node.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.besu.JsonRpc2_0Besu;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;

public class BesuNode implements Node {

  public static final String PROCESS_LOG_FILENAME = "subprocess.log";

  private static final Logger LOG = LogManager.getLogger();
  private static final Logger PROCESS_LOG =
      LogManager.getLogger("org.hyperledger.besu.SubProcessLog");
  /** Besu's dev.json has the hard fork at block 0 */
  private static final BigInteger SPURIOUS_DRAGON_HARD_FORK_BLOCK = BigInteger.valueOf(1);

  private final BesuNodeConfig besuNodeConfig;
  private final ProcessBuilder processBuilder;
  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();
  private final Properties portsProperties = new Properties();

  private Process process;
  private FileOutputStream logStream;

  private Accounts accounts;
  private Transactions transactions;
  private Web3j jsonRpc;
  private PublicContracts publicContracts;
  private PrivateContracts privateContracts;

  BesuNode(final BesuNodeConfig besuNodeConfig, final ProcessBuilder processBuilder) {
    this.besuNodeConfig = besuNodeConfig;
    this.processBuilder = processBuilder;
  }

  public String getP2pPort() {
    return getPortByName("p2p");
  }

  public String getDiscPort() {
    return getPortByName("discovery");
  }

  public String getJsonRpcPort() {
    return getPortByName("json-rpc");
  }

  public String getWSPort() {
    return getPortByName("ws-rpc");
  }

  private String getPortByName(final String name) {
    return Optional.ofNullable(portsProperties.getProperty(name))
        .orElseThrow(
            () -> new IllegalStateException("Requested Port before ports properties were written"));
  }

  public boolean logContains(final String expectedContent) {
    try {
      return Files.readAllLines(getLogPath()).stream()
          .anyMatch(line -> line.contains(expectedContent));
    } catch (final IOException e) {
      throw new RuntimeException("Unable to extract lines from log file");
    }
  }

  public Path getLogPath() {
    return besuNodeConfig.getDataPath().resolve(PROCESS_LOG_FILENAME);
  }

  public void ensureHasTerminated() {
    Awaitility.waitAtMost(60, TimeUnit.SECONDS).until(() -> process == null || !process.isAlive());
  }

  public void waitForLogToContain(final String requiredText) {
    final int secondsToWait = Boolean.getBoolean("debugSubProcess") ? 3600 : 60;
    Awaitility.waitAtMost(secondsToWait, TimeUnit.SECONDS)
        .until(() -> this.logContains(requiredText));
  }

  private void loadPortsFile() {
    try (final FileInputStream fis =
        new FileInputStream(new File(besuNodeConfig.getDataPath().toFile(), "besu.ports"))) {
      portsProperties.load(fis);
      LOG.info("Ports for node {}: {}", besuNodeConfig.getName(), portsProperties);
    } catch (final IOException e) {
      throw new RuntimeException("Error reading Besu ports file", e);
    }
  }

  public BigInteger blockNumber() {
    if (jsonRpc == null) {
      throw new RuntimeException("Requested block number prior to initializing web3j.");
    }

    try {
      return jsonRpc.ethBlockNumber().send().getBlockNumber();
    } catch (final IOException e) {
      LOG.error("Failed to determine block number of besu.");
      throw new RuntimeException(e);
    }
  }

  public BigInteger peerCount() {
    if (jsonRpc == null) {
      throw new RuntimeException("Requested peer count prior to initializing web3j.");
    }

    try {
      return jsonRpc.netPeerCount().send().getQuantity();
    } catch (final Exception e) {
      LOG.error("{}: Failed to determine peer count of besu.", besuNodeConfig.getName(), e);
      throw new RuntimeException(e);
    }
  }

  public void waitToHaveExpectedPeerCount(final int expectedPeerCount) {
    Awaitility.setDefaultPollInterval(500, TimeUnit.MILLISECONDS);
    Awaitility.waitAtMost(60, TimeUnit.SECONDS)
        .until(
            () -> {
              final BigInteger peerCount;
              try {
                peerCount = peerCount();
              } catch (final Exception e) {
                return false;
              }
              return peerCount.compareTo(BigInteger.valueOf(expectedPeerCount)) >= 0;
            });
  }

  @Override
  public void start() {
    try {
      process = processBuilder.start();
      logStream = createLogStream(besuNodeConfig.getDataPath());
      outputProcessorExecutor.execute(this::printOutput);
    } catch (IOException e) {
      LOG.error("Unable to start Besu process.", e);
      throw new UncheckedIOException(e);
    }
  }

  private FileOutputStream createLogStream(final Path dataPath) {
    try {
      return new FileOutputStream(dataPath.resolve(PROCESS_LOG_FILENAME).toFile());
    } catch (final FileNotFoundException e) {
      throw new RuntimeException("Unable to create the subprocess log.", e);
    }
  }

  private void printOutput() {
    try (final BufferedReader in =
        new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
      String line = in.readLine();
      while (line != null) {
        PROCESS_LOG.info("{}: {}", besuNodeConfig.getName(), line);
        logStream.write(String.format("%s%n", line).getBytes(UTF_8));
        logStream.flush();
        try {
          line = in.readLine();
        } catch (final IOException e) {
          if (process.isAlive()) {
            LOG.error(
                "Reading besu output stream failed even though the process is still alive ({})",
                e.getMessage());
          }
          return;
        }
      }
    } catch (final IOException e) {
      LOG.error("Failed to read output from process", e);
    }
  }

  @Override
  public void shutdown() {
    Awaitility.waitAtMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              if (process != null && process.isAlive()) {
                process.destroy();
                outputProcessorExecutor.shutdown();
                return false;
              } else {
                return true;
              }
            });

    ensureHasTerminated();
  }

  @Override
  public void awaitStartupCompletion() {
    final int secondsToWait = Boolean.getBoolean("debugSubProcess") ? 3600 : 60;

    final File file = new File(besuNodeConfig.getDataPath().toFile(), "besu.ports");
    Awaitility.waitAtMost(secondsToWait, TimeUnit.SECONDS)
        .until(
            () -> {
              if (file.exists()) {
                final boolean containsContent;
                try (final Stream<String> s = Files.lines(file.toPath())) {
                  containsContent = s.count() > 0;
                }
                if (containsContent) {
                  loadPortsFile();
                }
                return containsContent;
              } else {
                return false;
              }
            });
    final String web3jEndPointURL = String.format("http://localhost:%s", getJsonRpcPort());
    final Web3jService web3jService = new HttpService(web3jEndPointURL);
    jsonRpc = new JsonRpc2_0Web3j(web3jService);

    // wait for eth blocks
    try {
      LOG.info("Waiting for Besu to become responsive...");
      waitFor(60, () -> assertThat(jsonRpc.ethBlockNumber().send().hasError()).isFalse());
      LOG.info("Besu is now responsive");
      waitFor(
          () ->
              assertThat(jsonRpc.ethBlockNumber().send().getBlockNumber())
                  .isGreaterThan(SPURIOUS_DRAGON_HARD_FORK_BLOCK));
    } catch (final ConditionTimeoutException e) {
      throw new RuntimeException("Failed to start the Besu node", e);
    }

    final Eth eth = new Eth(jsonRpc);
    final Besu besu = new Besu(new JsonRpc2_0Besu(web3jService));
    final Eea eea = new Eea(new RawJsonRpcRequestFactory(web3jService));
    this.accounts = new Accounts(eth);
    this.publicContracts = new PublicContracts(eth);
    this.privateContracts = new PrivateContracts(besu, eea);
    this.transactions = new Transactions(eth);
  }

  @Override
  public BesuNodePorts ports() {
    return new BesuNodePorts(Integer.parseInt(getJsonRpcPort()), Integer.parseInt(getWSPort()));
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
}

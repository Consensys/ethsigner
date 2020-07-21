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
package tech.pegasys.ethsigner.tests.dsl.signer;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.core.config.ClientAuthConstraints;
import tech.pegasys.ethsigner.core.config.TlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodePorts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public abstract class EthSignerRunner {

  private static final Logger LOG = LogManager.getLogger();

  private static final String PORTS_FILENAME = "ethsigner.ports";
  private static final String HTTP_JSON_RPC_KEY = "http-jsonrpc";

  private final Properties portsProperties;
  private final String nodeHostname;
  private final String nodeHttpRpcPort;
  private final String timeoutMilliseconds;
  private final String signerHostname;
  private final String chainId;
  private final int signerHttpRpcPort;

  protected final Path dataPath;
  protected final boolean useDynamicPortAllocation;
  protected final SignerConfiguration signerConfig;

  public static EthSignerRunner createRunner(
      final SignerConfiguration signerConfig,
      final String nodeHostname,
      final BesuNodePorts besuNodePorts) {
    if (Boolean.getBoolean("acctests.runEthSignerAsProcess")) {
      LOG.info("EthSigner running as a process.");
      return new EthSignerProcessRunner(signerConfig, nodeHostname, besuNodePorts);
    } else {
      LOG.info("EthSigner running in a thread.");
      return new EthSignerThreadRunner(signerConfig, nodeHostname, besuNodePorts);
    }
  }

  public EthSignerRunner(
      final SignerConfiguration signerConfig,
      final String nodeHostname,
      final BesuNodePorts besuNodePorts) {

    this.nodeHostname = nodeHostname;
    this.nodeHttpRpcPort = String.valueOf(besuNodePorts.getHttpRpc());
    this.timeoutMilliseconds = String.valueOf(signerConfig.timeout().toMillis());
    this.signerHostname = signerConfig.hostname();
    this.signerHttpRpcPort = signerConfig.httpRpcPort();
    this.chainId = signerConfig.chainId();
    this.portsProperties = new Properties();
    this.signerConfig = signerConfig;

    this.useDynamicPortAllocation = signerConfig.isDynamicPortAllocation();

    if (useDynamicPortAllocation) {
      try {
        dataPath = Files.createTempDirectory("acceptance-test");
        FileUtils.forceDeleteOnExit(dataPath.toFile());
      } catch (final IOException e) {
        throw new RuntimeException(
            "Failed to create the temporary directory to store the ethsigner.ports file");
      }
    } else {
      dataPath = null;
    }
  }

  public void start(final String processName) {
    final String loggingLevel = "DEBUG";

    final List<String> params = new ArrayList<>();
    params.add("--logging");
    params.add(loggingLevel);
    params.add("--downstream-http-host");
    params.add(nodeHostname);
    params.add("--downstream-http-port");
    params.add(nodeHttpRpcPort);
    params.add("--downstream-http-request-timeout");
    params.add(timeoutMilliseconds);
    params.add("--http-listen-host");
    params.add(signerHostname);
    params.add("--http-listen-port");
    params.add(String.valueOf(signerHttpRpcPort));
    params.add("--chain-id");
    params.add(chainId);
    if (useDynamicPortAllocation) {
      params.add("--data-path");
      params.add(dataPath.toAbsolutePath().toString());
    }

    params.addAll(createServerTlsArgs());
    params.addAll(createDownstreamTlsArgs());

    params.addAll(signerConfig.transactionSignerParamsSupplier().get());

    LOG.info("Creating EthSigner process with params {}", params);

    launchEthSigner(params, processName);

    if (useDynamicPortAllocation) {
      loadPortsFile();
    }
  }

  private Collection<? extends String> createServerTlsArgs() {
    final List<String> params = Lists.newArrayList();

    if (signerConfig.serverTlsOptions().isPresent()) {
      final TlsOptions serverTlsOptions = signerConfig.serverTlsOptions().get();
      params.add("--tls-keystore-file");
      params.add(serverTlsOptions.getKeyStoreFile().toString());
      params.add("--tls-keystore-password-file");
      params.add(serverTlsOptions.getKeyStorePasswordFile().toString());
      if (serverTlsOptions.getClientAuthConstraints().isEmpty()) {
        params.add("--tls-allow-any-client");
      } else {
        final ClientAuthConstraints constraints = serverTlsOptions.getClientAuthConstraints().get();
        if (constraints.getKnownClientsFile().isPresent()) {
          params.add("--tls-known-clients-file");
          params.add(constraints.getKnownClientsFile().get().toString());
        }
        if (constraints.isCaAuthorizedClientAllowed()) {
          params.add("--tls-allow-ca-clients");
        }
      }
    }
    return params;
  }

  private Collection<String> createDownstreamTlsArgs() {
    final Optional<ClientTlsOptions> optionalClientTlsOptions = signerConfig.clientTlsOptions();
    if (optionalClientTlsOptions.isEmpty()) {
      return Collections.emptyList();
    }

    final List<String> params = new ArrayList<>();
    params.add("--downstream-http-tls-enabled");

    final ClientTlsOptions clientTlsOptions = optionalClientTlsOptions.get();
    clientTlsOptions
        .getKeyStoreOptions()
        .ifPresent(
            pkcsStoreConfig -> {
              params.add("--downstream-http-tls-keystore-file");
              params.add(pkcsStoreConfig.getKeyStoreFile().toString());
              params.add("--downstream-http-tls-keystore-password-file");
              params.add(pkcsStoreConfig.getPasswordFile().toString());
            });

    if (clientTlsOptions.getKnownServersFile().isPresent()) {
      params.add("--downstream-http-tls-known-servers-file");
      params.add(clientTlsOptions.getKnownServersFile().get().toAbsolutePath().toString());
    }
    if (!clientTlsOptions.isCaAuthEnabled()) {
      params.add("--downstream-http-tls-ca-auth-enabled");
      params.add("false");
    }

    return Collections.unmodifiableCollection(params);
  }

  public int httpJsonRpcPort() {
    if (useDynamicPortAllocation) {
      final String value = portsProperties.getProperty(HTTP_JSON_RPC_KEY);
      LOG.info("{}: {}", HTTP_JSON_RPC_KEY, value);
      assertThat(value).isNotEmpty();
      return Integer.parseInt(value);
    } else {
      return signerHttpRpcPort;
    }
  }

  private void loadPortsFile() {
    final File portsFile = new File(dataPath.toFile(), PORTS_FILENAME);
    LOG.info("Awaiting presence of ethsigner.ports file: {}", portsFile.getAbsolutePath());
    awaitPortsFile(dataPath);
    LOG.info("Found ethsigner.ports file: {}", portsFile.getAbsolutePath());

    try (final FileInputStream fis = new FileInputStream(portsFile)) {
      portsProperties.load(fis);
      LOG.info("EthSigner ports: {}", portsProperties);
    } catch (final IOException e) {
      throw new RuntimeException("Error reading Web3Provider ports file", e);
    }
  }

  private void awaitPortsFile(final Path dataDir) {
    final int secondsToWait = Boolean.getBoolean("debugSubProcess") ? 3600 : 30;
    final File file = new File(dataDir.toFile(), PORTS_FILENAME);
    Awaitility.waitAtMost(secondsToWait, TimeUnit.SECONDS)
        .until(
            () -> {
              if (file.exists()) {
                try (final Stream<String> s = Files.lines(file.toPath())) {
                  return s.count() > 0;
                }
              }
              return false;
            });
  }

  public abstract void launchEthSigner(final List<String> params, final String processName);

  public abstract boolean isRunning();

  public abstract void shutdown();
}

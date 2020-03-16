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
package tech.pegasys.ethsigner.tests.dsl.signer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.createJksTrustStore;

import tech.pegasys.ethsigner.core.config.ClientAuthConstraints;
import tech.pegasys.ethsigner.core.config.TlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public class EthSignerProcessRunner {

  private static final Logger LOG = LogManager.getLogger();
  private static final Logger PROCESS_LOG =
      LogManager.getLogger("tech.pegasys.ethsigner.SubProcessLog");

  private static final String PORTS_FILENAME = "ethsigner.ports";
  private static final String HTTP_JSON_RPC_KEY = "http-jsonrpc";

  private final Map<String, Process> processes = new HashMap<>();
  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();
  private final Properties portsProperties;
  private final String nodeHostname;
  private final String nodeHttpRpcPort;
  private final String timeoutMilliseconds;
  private final String signerHostname;
  private final String chainId;
  private final boolean useDynamicPortAllocation;
  private final Path dataPath;
  private final int signerHttpRpcPort;
  private final SignerConfiguration signerConfig;

  public EthSignerProcessRunner(
      final SignerConfiguration signerConfig,
      final NodeConfiguration nodeConfig,
      final NodePorts nodePorts) {
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

    this.nodeHostname = nodeConfig.getHostname();
    this.nodeHttpRpcPort = String.valueOf(nodePorts.getHttpRpc());
    this.timeoutMilliseconds = String.valueOf(signerConfig.timeout().toMillis());
    this.signerHostname = signerConfig.hostname();
    this.signerHttpRpcPort = signerConfig.httpRpcPort();
    this.chainId = signerConfig.chainId();
    this.portsProperties = new Properties();
    this.signerConfig = signerConfig;

    this.useDynamicPortAllocation = signerConfig.isDynamicPortAllocation();

    if (useDynamicPortAllocation) {
      try {
        this.dataPath = Files.createTempDirectory("acceptance-test");
      } catch (final IOException e) {
        throw new RuntimeException(
            "Failed to create the temporary directory to store the ethsigner.ports file");
      }
    } else {
      dataPath = null;
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  public synchronized void shutdown() {
    final HashMap<String, Process> localMap = new HashMap<>(processes);
    localMap.forEach(this::killProcess);
    outputProcessorExecutor.shutdown();
    try {
      if (!outputProcessorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        LOG.error("Output processor executor did not shutdown cleanly.");
      }
    } catch (final InterruptedException e) {
      LOG.error("Interrupted while already shutting down", e);
      Thread.currentThread().interrupt();
    } finally {
      if (useDynamicPortAllocation) {
        try {
          MoreFiles.deleteRecursively(dataPath, RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (final IOException e) {
          LOG.info("Failed to clean up temporary file: {}", dataPath, e);
        }
      }
    }
  }

  public void start(final String processName) {
    final String loggingLevel = "DEBUG";

    final List<String> params = new ArrayList<>();
    params.add(executableLocation());
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

    final StringJoiner javaOpts = new StringJoiner(" ");

    if (signerConfig.getOverriddenCaTrustStore().isPresent()) {
      final TlsCertificateDefinition overriddenCaTrustStore =
          signerConfig.getOverriddenCaTrustStore().get();
      final Path overriddenCaTrustStorePath = createJksTrustStore(dataPath, overriddenCaTrustStore);
      javaOpts.add(
          "-Djavax.net.ssl.trustStore=" + overriddenCaTrustStorePath.toAbsolutePath().toString());
      javaOpts.add("-Djavax.net.ssl.trustStorePassword=" + overriddenCaTrustStore.getPassword());
    }

    params.addAll(signerConfig.transactionSignerParamsSupplier().get());

    LOG.info("Creating EthSigner process with params {}", params);

    final ProcessBuilder processBuilder =
        new ProcessBuilder(params)
            .directory(new File(System.getProperty("user.dir")).getParentFile())
            .redirectErrorStream(true)
            .redirectInput(Redirect.INHERIT);

    if (Boolean.getBoolean("debugSubProcess")) {
      javaOpts.add("-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
    }
    processBuilder.environment().put("JAVA_OPTS", javaOpts.toString());

    try {
      final Process process = processBuilder.start();
      outputProcessorExecutor.submit(() -> printOutput(processName, process));
      processes.put(processName, process);
    } catch (final IOException e) {
      LOG.error("Error starting EthSigner process", e);
      throw new RuntimeException("Failed to start the Ethsigner process");
    }

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

  public boolean isRunning(final String processName) {
    return (processes.get(processName) != null) && processes.get(processName).isAlive();
  }

  private String executableLocation() {
    return "build/install/ethsigner/bin/ethsigner";
  }

  private void printOutput(final String name, final Process process) {
    try (final BufferedReader in =
        new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
      String line = in.readLine();
      while (line != null) {
        PROCESS_LOG.info("{}: {}", name, line);
        line = in.readLine();
      }
    } catch (final IOException e) {
      LOG.error("Failed to read output from process", e);
    }
  }

  private void killProcess(final String name, final Process process) {
    LOG.info("Killing {} process", name);

    Awaitility.waitAtMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              if (process.isAlive()) {
                process.destroy();
                processes.remove(name);
                return false;
              } else {
                processes.remove(name);
                return true;
              }
            });
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
}

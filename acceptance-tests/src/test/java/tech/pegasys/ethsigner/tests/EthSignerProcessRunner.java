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

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.tests.dsl.Accounts;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public class EthSignerProcessRunner {

  private static final Logger LOG = LogManager.getLogger();
  private static final Logger PROCESS_LOG =
      LogManager.getLogger("tech.pegasys.ethsigner.SubProcessLog");

  private final Map<String, Process> processes = new HashMap<>();
  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();
  private final String nodeHostname;
  private final String nodeTcpPort;
  private final String timeoutMs;
  private final String signerHostname;
  private final String signerPort;
  private final String chainId;

  public EthSignerProcessRunner(
      final SignerConfiguration signerConfig, final NodeConfiguration nodeConfig) {
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

    this.nodeHostname = nodeConfig.getHostname();
    this.nodeTcpPort = String.valueOf(nodeConfig.getTcpPort());
    this.timeoutMs = String.valueOf(nodeConfig.getPollingInterval().toMillis());
    this.signerHostname = signerConfig.hostname();
    this.signerPort = String.valueOf(signerConfig.tcpPort());
    this.chainId = signerConfig.chainId();
  }

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
    }
  }

  public void start(final String processName) {
    final String loggingLevel = "DEBUG";

    final List<String> params = new ArrayList<>();
    params.add(executableLocation());
    params.add("--logging");
    params.add(loggingLevel);
    params.add("--password-file");
    params.add(createPasswordFile().getAbsolutePath());
    params.add("--key-file");
    params.add(createKeyFile().getAbsolutePath());
    params.add("--downstream-http-host");
    params.add(nodeHostname);
    params.add("--downstream-http-port");
    params.add(nodeTcpPort);
    params.add("--downstream-http-request-timeout");
    params.add(timeoutMs);
    params.add("--http-listen-host");
    params.add(signerHostname);
    params.add("--http-listen-port");
    params.add(signerPort);
    params.add("--chain-id");
    params.add(chainId);

    LOG.info("Creating EthSigner process with params {}", params);

    final ProcessBuilder processBuilder =
        new ProcessBuilder(params)
            .directory(new File(System.getProperty("user.dir")).getParentFile())
            .redirectErrorStream(true)
            .redirectInput(Redirect.INHERIT);

    try {
      final Process process = processBuilder.start();
      outputProcessorExecutor.submit(() -> printOutput(processName, process));
      processes.put(processName, process);
    } catch (final IOException e) {
      LOG.error("Error starting EthSigner process", e);
    }
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

  private File createPasswordFile() {
    return createJsonFile(
        "ethsigner_passwordfile", Accounts.GENESIS_ACCOUNT_ONE_PASSWORD.getBytes(UTF_8));
  }

  @SuppressWarnings("UnstableApiUsage")
  private File createKeyFile() {
    final URL resource = Resources.getResource("rich_benefactor_one.json");
    final byte[] data;

    try {
      data = Resources.toString(resource, UTF_8).getBytes(UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    return createJsonFile("ethsigner_keyfile", data);
  }

  @SuppressWarnings("UnstableApiUsage")
  private File createJsonFile(final String tempNamePrefix, final byte[] data) {
    final Path file;
    try {
      file = Files.createTempFile(tempNamePrefix, ".json");
      Files.write(file, data);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    File keyFile = file.toFile();
    keyFile.deleteOnExit();
    return keyFile;
  }
}

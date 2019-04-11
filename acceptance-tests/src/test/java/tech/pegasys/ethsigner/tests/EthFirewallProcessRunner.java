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

public class EthFirewallProcessRunner {

  private static final Logger LOG = LogManager.getLogger();
  private static final Logger PROCESS_LOG =
      LogManager.getLogger("tech.pegasys.ethsigner.SubProcessLog");

  private final Map<String, Process> processes = new HashMap<>();
  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();

  EthFirewallProcessRunner() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
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

  public void start(final String name) {

    final List<String> params = new ArrayList<>();
    params.add(executableLocation());
    params.add("--logging INFO");
    params.add("--password-file");
    params.add(createPasswordFile().getAbsolutePath());
    params.add("--key-file");
    params.add(createKeyFile().getAbsolutePath());
    params.add("--downstream-http-host 127.0.0.1");
    params.add("--downstream-http-port 8545");
    params.add("--downstream-http-request-timeout 500");
    params.add("--http-listen-host 127.0.0.1");
    params.add("--http-listen-port 9945");
    params.add("--chain-id 3216547778");

    LOG.info("Creating EthSigner process with params {}", params);

    final ProcessBuilder processBuilder =
        new ProcessBuilder(params)
            .directory(new File(System.getProperty("user.dir")).getParentFile())
            .redirectErrorStream(true)
            .redirectInput(Redirect.INHERIT);

    try {
      final Process process = processBuilder.start();
      outputProcessorExecutor.submit(() -> printOutput(name, process));
      processes.put(name, process);
    } catch (final IOException e) {
      LOG.error("Error starting EthSigner process", e);
    }
  }

  private String executableLocation() {
    return "build/install/ethfirewall/bin/ethfirewall";
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

  @SuppressWarnings("UnstableApiUsage")
  private static File createKeyFile() {
    final Path wallet;
    try {
      final URL walletResource = Resources.getResource("rich_benefactor_one.json");
      wallet = Files.createTempFile("ethsigner_into_keyfile", ".json");
      Files.write(wallet, Resources.toString(walletResource, UTF_8).getBytes(UTF_8));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    File keyFile = wallet.toFile();
    keyFile.deleteOnExit();
    return keyFile;
  }

  @SuppressWarnings("UnstableApiUsage")
  private static File createPasswordFile() {
    final Path wallet;
    try {
      wallet = Files.createTempFile("ethsigner_into_passwordfile", ".json");
      Files.write(wallet, "pass".getBytes(UTF_8));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    File keyFile = wallet.toFile();
    keyFile.deleteOnExit();
    return keyFile;
  }
}

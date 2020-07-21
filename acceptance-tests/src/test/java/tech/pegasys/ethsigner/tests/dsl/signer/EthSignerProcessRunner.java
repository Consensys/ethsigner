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
import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.createJksTrustStore;

import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodePorts;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public class EthSignerProcessRunner extends EthSignerRunner {

  private static final Logger LOG = LogManager.getLogger();
  private static final Logger PROCESS_LOG =
      LogManager.getLogger("tech.pegasys.ethsigner.SubProcessLog");

  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();

  private Optional<Process> process = Optional.empty();

  public EthSignerProcessRunner(
      final SignerConfiguration signerConfig,
      final String hostName,
      final BesuNodePorts besuNodePorts) {
    super(signerConfig, hostName, besuNodePorts);

    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  private String executableLocation() {
    return "build/install/ethsigner/bin/ethsigner";
  }

  @Override
  public void launchEthSigner(final List<String> params, final String processName) {
    final String[] paramsAsArray = params.toArray(new String[0]);
    final List<String> paramsWithCmd = Lists.asList(executableLocation(), paramsAsArray);

    final ProcessBuilder processBuilder =
        new ProcessBuilder(paramsWithCmd)
            .directory(new File(System.getProperty("user.dir")).getParentFile())
            .redirectErrorStream(true)
            .redirectInput(Redirect.INHERIT);

    final StringJoiner javaOpts = new StringJoiner(" ");

    if (signerConfig.getOverriddenCaTrustStore().isPresent()) {
      final TlsCertificateDefinition overriddenCaTrustStore =
          signerConfig.getOverriddenCaTrustStore().get();
      final Path overriddenCaTrustStorePath = createJksTrustStore(dataPath, overriddenCaTrustStore);
      javaOpts.add(
          "-Djavax.net.ssl.trustStore=" + overriddenCaTrustStorePath.toAbsolutePath().toString());
      javaOpts.add("-Djavax.net.ssl.trustStorePassword=" + overriddenCaTrustStore.getPassword());
    }

    if (Boolean.getBoolean("debugSubProcess")) {
      javaOpts.add("-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
    }
    processBuilder.environment().put("JAVA_OPTS", javaOpts.toString());

    try {
      final Process process = processBuilder.start();
      outputProcessorExecutor.submit(() -> printOutput(processName, process));
      this.process = Optional.of(process);
    } catch (final IOException e) {
      LOG.error("Error starting EthSigner process", e);
      throw new RuntimeException("Failed to start the Ethsigner process");
    }
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

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public synchronized void shutdown() {
    killProcess();
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

  @Override
  public boolean isRunning() {
    return (process.isPresent() && process.get().isAlive());
  }

  private void killProcess() {
    LOG.info("Killing process");

    Awaitility.waitAtMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              if (isRunning()) {
                process.get().destroy();
                process = Optional.empty();
                return false;
              } else {
                process = Optional.empty();
                return true;
              }
            });
  }
}

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

import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BesuNodeFactory {
  private static final Logger LOG = LogManager.getLogger();
  private static final Path runDirectory = Path.of(System.getProperty("user.dir"));
  private static final Path besuInstallDir =
      Path.of(System.getProperty("besuInstallDir", runDirectory.resolve("build/besu").toString()));
  private static final Path executablePath = besuInstallDir.resolve("bin/besu");

  public static BesuNode create(final BesuNodeConfig config) {
    checkBesuInstallation();

    final List<String> params = Lists.newArrayList();
    params.add(executablePath.toString());
    if (config.getGenesisFile().isEmpty()) {
      params.add("--network");
      params.add("DEV");
    } else {
      params.add("--genesis-file");
      params.add(Resources.getResource(config.getGenesisFile().get()).getPath());
    }
    params.add("--data-path");
    params.add(config.getDataPath().toString());
    params.add("--logging=DEBUG");
    params.add("--miner-enabled");
    params.add("--miner-coinbase");
    params.add("1b23ba34ca45bb56aa67bc78be89ac00ca00da00");
    params.add("--host-whitelist");
    params.add("*");
    params.add("--p2p-port=0");
    params.add("--rpc-http-enabled=true");
    params.add("--rpc-http-port=0");
    params.add("--rpc-http-host=" + config.getHostName());
    params.add("--rpc-ws-enabled=true");
    params.add("--rpc-ws-port=0");
    params.add("--rpc-http-host=" + config.getHostName());
    params.add("--rpc-http-apis");
    params.add("ETH,NET,WEB3,EEA");
    params.add("--privacy-enabled");
    params.add("--privacy-public-key-file");
    params.add(privacyPublicKeyFilePath());

    config.getCors().ifPresent(cors -> params.addAll(List.of("--rpc-http-cors-origins", cors)));

    params.addAll(config.getAdditionalCommandLineArgs());

    final ProcessBuilder processBuilder = createBesuProcessBuilder(params, config);

    return new BesuNode(config, processBuilder);
  }

  private static void checkBesuInstallation() {
    LOG.info("Run Dir: {}", runDirectory);
    LOG.info("Besu Install Dir: {}", besuInstallDir);
    LOG.info("Executable Path: {}", executablePath);
    LOG.info("Exists? {}", executablePath.toFile().exists());

    if (!executablePath.toFile().exists()) {
      LOG.error(
          "Besu binary doesn't exist. Either run 'gradle extractBesu' or set system property 'besuInstallDir'");
      throw new IllegalStateException("Besu binary doesn't exist " + executablePath);
    }
  }

  private static ProcessBuilder createBesuProcessBuilder(
      final List<String> commandLine, final BesuNodeConfig config) {

    final ProcessBuilder processBuilder =
        new ProcessBuilder(commandLine)
            .directory(runDirectory.toFile())
            .redirectErrorStream(true)
            .redirectInput(Redirect.INHERIT);

    for (final String envVarToRemove : config.getEnvironmentVariablesToRemove()) {
      processBuilder.environment().remove(envVarToRemove);
    }

    final StringBuilder javaOptions = new StringBuilder();

    if (Boolean.getBoolean("debugSubProcess")) {
      javaOptions.append(
          "-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
      javaOptions.append(" ");
      processBuilder.environment().put("JAVA_OPTS", javaOptions.toString());
    }

    return processBuilder;
  }

  private static String privacyPublicKeyFilePath() {
    final URL resource = Resources.getResource("enclave_key.pub");
    return resource.getPath();
  }
}

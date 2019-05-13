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
package tech.pegasys.ethsigner.main;

import tech.pegasys.ethsigner.EthSigner;
import tech.pegasys.ethsigner.RunnerBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Main {

  private static final Logger LOG = LogManager.getLogger();

  public static void main(final String... args) {
    final CommandLineConfig config = new CommandLineConfig(System.out);
    if (!config.parse(args)) {
      return;
    }

    // set log level per CLI flags
    System.out.println("Setting logging level to " + config.getLogLevel().name());
    Configurator.setAllLevels("", config.getLogLevel());

    LOG.debug("Configuration = {}", config);
    LOG.info("Version = {}, ", ApplicationInfo.version());

    final EthSigner ethSigner = new EthSigner(config, new RunnerBuilder());
    ethSigner.run();
  }
}

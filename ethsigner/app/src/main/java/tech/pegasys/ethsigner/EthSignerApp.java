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
package tech.pegasys.ethsigner;

import tech.pegasys.ethsigner.core.RunnerBuilder;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.fileBased.FileBasedSignerBuilder;
import tech.pegasys.ethsigner.core.signing.hashicorp.HashicorpSignerBuilder;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class EthSignerApp {

  private static final Logger LOG = LogManager.getLogger();

  private static final Vertx vertx = Vertx.vertx();

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

    // create a signer based on the configuration provided
    final TransactionSigner signer = createTransactionSigner(config);

    if (signer == null) {
      LOG.error("Cannot create a signer from the given config: " + config.toString());
      System.exit(-1);
    }

    final tech.pegasys.ethsigner.core.EthSigner ethSigner =
        new tech.pegasys.ethsigner.core.EthSigner(config, signer, vertx, new RunnerBuilder());
    ethSigner.run();
  }

  private static TransactionSigner createTransactionSigner(final CommandLineConfig config) {
    TransactionSigner signer = null;
    if (config.getHashicorpSignerConfig().isConfigured()) {
      signer = new HashicorpSignerBuilder(config.getHashicorpSignerConfig(), vertx).build();
    } else if (config.getFileBasedSignerConfig().isConfigured()) {
      signer = new FileBasedSignerBuilder(config.getFileBasedSignerConfig()).build();
    }
    return signer;
  }
}

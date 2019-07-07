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

import tech.pegasys.ethsigner.core.EthSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

public abstract class SignerSubCommand implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  @CommandLine.ParentCommand private EthSignerBaseCommand config;

  public abstract TransactionSigner createSigner() throws TransactionSignerInitializationException;

  public abstract String getCommandName();

  @Override
  public void run() throws TransactionSignerInitializationException {
    // set log level per CLI flags
    System.out.println("Setting logging level to " + config.getLogLevel().name());
    Configurator.setAllLevels("", config.getLogLevel());

    LOG.debug("Configuration = {}", this);
    LOG.info("Version = {}", ApplicationInfo.version());

    final TransactionSigner transactionSigner = createSigner();

    final EthSigner signer = new EthSigner(config, transactionSigner);
    signer.run();
  }
}

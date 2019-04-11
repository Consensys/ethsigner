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
package tech.pegasys.ethfirewall;

import tech.pegasys.ethfirewall.signing.ChainIdProvider;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import com.google.common.base.Charsets;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

public final class EthFirewall {

  private static final Logger LOG = LoggerFactory.getLogger(EthFirewall.class);

  private final EthFirewallConfig config;
  private final RunnerBuilder runnerBuilder;

  public EthFirewall(final EthFirewallConfig config, final RunnerBuilder runnerBuilder) {
    this.config = config;
    this.runnerBuilder = runnerBuilder;
  }

  public void run() {
    // set log level per CLI flags
    System.out.println("Setting logging level to " + config.getLogLevel().name());
    Configurator.setAllLevels("", config.getLogLevel());

    Optional<String> password = readPasswordFromFile();
    if (!password.isPresent()) {
      LOG.error("Unable to extract password from supplied password file.");
      return;
    }

    if (config.getDownstreamHttpRequestTimeout().toMillis() <= 0) {
      LOG.error("Http request timeout must be greater than 0.");
      return;
    }

    if (config.getHttpListenHost().equals(config.getDownstreamHttpHost())
        && config.getHttpListenPort().equals(config.getDownstreamHttpPort())) {
      LOG.error("Http host and port must be different to the downstream host and port.");
      return;
    }

    try {
      runnerBuilder.setTransactionSigner(
          transactionSigner(config.getKeyPath().toFile(), password.get(), config.getChainId()));
      runnerBuilder.setClientOptions(
          new WebClientOptions()
              .setDefaultPort(config.getDownstreamHttpPort())
              .setDefaultHost(config.getDownstreamHttpHost().getHostAddress()));
      runnerBuilder.setServerOptions(
          new HttpServerOptions()
              .setPort(config.getHttpListenPort())
              .setHost(config.getHttpListenHost().getHostAddress())
              .setReuseAddress(true)
              .setReusePort(true));
      runnerBuilder.setHttpRequestTimeout(config.getDownstreamHttpRequestTimeout());
      runnerBuilder.build().start();
    } catch (IOException ex) {
      LOG.info(
          "Unable to access supplied keyfile, or file does not conform to V3 keystore standard.");
    } catch (CipherException ex) {
      LOG.info("Unable to decode keyfile with supplied passwordFile.");
    }
  }

  private TransactionSigner transactionSigner(
      final File keyFile, final String password, final ChainIdProvider chain)
      throws IOException, CipherException {
    final Credentials credentials = WalletUtils.loadCredentials(password, keyFile);

    return new TransactionSigner(chain, credentials, new RawTransactionConverter());
  }

  private Optional<String> readPasswordFromFile() {
    try {
      byte[] fileContent = Files.readAllBytes(config.getPasswordFilePath());
      return Optional.of(new String(fileContent, Charsets.UTF_8));
    } catch (IOException ex) {
      LOG.debug("Failed to read password from password file, {}", ex);
      return Optional.empty();
    }
  }

  public static void main(final String... args) {
    final EthFirewallCommandLineConfig config = new EthFirewallCommandLineConfig(System.out);
    if (!config.parse(args)) {
      return;
    }

    final EthFirewall ethFirewall = new EthFirewall(config, new RunnerBuilder());
    ethFirewall.run();
  }
}

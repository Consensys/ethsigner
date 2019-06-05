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
package tech.pegasys.ethsigner.core;

import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.ethsigner.core.signing.FileBasedTransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import com.google.common.base.Charsets;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClientOptions;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.eea.Eea;
import org.web3j.protocol.eea.JsonRpc2_0Eea;
import org.web3j.protocol.http.HttpService;

public final class EthSigner {

  private static final Logger LOG = LogManager.getLogger();

  private final Config config;
  private final RunnerBuilder runnerBuilder;

  public EthSigner(final Config config, final RunnerBuilder runnerBuilder) {
    this.config = config;
    this.runnerBuilder = runnerBuilder;
  }

  public void run() {

    final Optional<String> password = readPasswordFromFile();
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

      final HttpService web3jService = createWeb3jHttpService();
      final Web3j web3j = new JsonRpc2_0Web3j(web3jService);
      final Eea eea = new JsonRpc2_0Eea(web3jService);

      final FileBasedTransactionSigner signer =
          FileBasedTransactionSigner.createFrom(config.getKeyPath().toFile(), password.get());

      runnerBuilder
          .withTransactionSerialiser(new TransactionSerialiser(signer, config.getChainId().id()))
          .withClientOptions(
              new WebClientOptions()
                  .setDefaultPort(config.getDownstreamHttpPort())
                  .setDefaultHost(config.getDownstreamHttpHost().getHostAddress()))
          .withServerOptions(
              new HttpServerOptions()
                  .setPort(config.getHttpListenPort())
                  .setHost(config.getHttpListenHost().getHostAddress())
                  .setReuseAddress(true)
                  .setReusePort(true))
          .withHttpRequestTimeout(config.getDownstreamHttpRequestTimeout())
          .withTransactionFactory(new TransactionFactory(eea, web3j))
          .withDataPath(config.getDataDirectory())
          .build()
          .start();
    } catch (final IOException ex) {
      LOG.info(
          "Unable to access supplied keyfile, or file does not conform to V3 keystore standard.");
    } catch (final CipherException ex) {
      LOG.info("Unable to decode keyfile with supplied passwordFile.");
    }
  }

  private HttpService createWeb3jHttpService() {
    final String downstreamUrl =
        "http://"
            + config.getDownstreamHttpHost().getHostName()
            + ":"
            + config.getDownstreamHttpPort();
    LOG.info("Downstream URL = {}", downstreamUrl);

    final OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder
        .connectTimeout(config.getDownstreamHttpRequestTimeout())
        .readTimeout(config.getDownstreamHttpRequestTimeout());
    return new HttpService(downstreamUrl, builder.build());
  }

  private Optional<String> readPasswordFromFile() {
    try {
      final byte[] fileContent = Files.readAllBytes(config.getPasswordFilePath());
      return Optional.of(new String(fileContent, Charsets.UTF_8));
    } catch (final IOException ex) {
      LOG.debug("Failed to read password from password file: {}", config.getPasswordFilePath(), ex);
      return Optional.empty();
    }
  }
}

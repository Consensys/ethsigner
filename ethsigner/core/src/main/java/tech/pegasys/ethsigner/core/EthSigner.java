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
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.nio.file.Path;
import java.time.Duration;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClientOptions;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.pantheon.Pantheon;

public final class EthSigner {

  private static final Logger LOG = LogManager.getLogger();

  private final Config config;
  private final TransactionSigner signer;

  public EthSigner(final Config config, final TransactionSigner signer) {
    this.config = config;
    this.signer = signer;
  }

  public void run() {

    final Duration downstreamHttpRequestTimeout = config.getDownstreamHttpRequestTimeout();
    if (downstreamHttpRequestTimeout.toMillis() <= 0) {
      LOG.error("Http request timeout must be greater than 0.");
      return;
    }

    if (config.getHttpListenHost().equals(config.getDownstreamHttpHost())
        && config.getHttpListenPort().equals(config.getDownstreamHttpPort())) {
      LOG.error("Http host and port must be different to the downstream host and port.");
      return;
    }

    final HttpService web3jService = createWeb3jHttpService();
    final Web3j web3j = new JsonRpc2_0Web3j(web3jService);
    final Pantheon pantheon = Pantheon.build(web3jService);
    final TransactionFactory transactionFactory =
        new TransactionFactory(pantheon, web3j, web3jService);
    final TransactionSerialiser serialiser =
        new TransactionSerialiser(signer, config.getChainId().id());
    final WebClientOptions clientOptions =
        new WebClientOptions()
            .setDefaultPort(config.getDownstreamHttpPort())
            .setDefaultHost(config.getDownstreamHttpHost().getHostAddress());
    final HttpServerOptions serverOptions =
        new HttpServerOptions()
            .setPort(config.getHttpListenPort())
            .setHost(config.getHttpListenHost().getHostAddress())
            .setReuseAddress(true)
            .setReusePort(true);
    final Path dataPath = config.getDataPath();

    final Runner runner =
        new Runner(
            serialiser,
            clientOptions,
            serverOptions,
            downstreamHttpRequestTimeout,
            transactionFactory,
            dataPath);

    runner.start();
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
}

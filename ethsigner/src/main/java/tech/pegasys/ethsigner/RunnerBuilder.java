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

import tech.pegasys.ethsigner.requesthandler.sendtransaction.NonceProvider;
import tech.pegasys.ethsigner.signing.TransactionSerialiser;

import java.time.Duration;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(RunnerBuilder.class);

  private TransactionSerialiser serialiser;
  private WebClientOptions clientOptions;
  private HttpServerOptions serverOptions;
  private Duration requestTimeout;
  private NonceProvider nonceProvider;

  public RunnerBuilder() {}

  public RunnerBuilder setTransactionSerialiser(final TransactionSerialiser serialiser) {
    this.serialiser = serialiser;
    return this;
  }

  public RunnerBuilder setClientOptions(final WebClientOptions clientOptions) {
    this.clientOptions = clientOptions;
    return this;
  }

  public RunnerBuilder setServerOptions(final HttpServerOptions serverOptions) {
    this.serverOptions = serverOptions;
    return this;
  }

  public RunnerBuilder setHttpRequestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  public RunnerBuilder setNonceProvider(final NonceProvider nonceProvider) {
    this.nonceProvider = nonceProvider;
    return this;
  }

  public Runner build() {
    if (serialiser == null) {
      LOG.error("Unable to construct Runner, transactionSerialiser is unset.");
      return null;
    }
    if (clientOptions == null) {
      LOG.error("Unable to construct Runner, clientOptions is unset.");
      return null;
    }
    if (serverOptions == null) {
      LOG.error("Unable to construct Runner, serverOptions is unset.");
      return null;
    }

    if (requestTimeout == null) {
      LOG.error("Unable to construct Runner, requestTimeout is unset.");
      return null;
    }

    if (nonceProvider == null) {
      LOG.error("Unable to construct Runner, nonceProvider is unset.");
      return null;
    }
    return new Runner(serialiser, clientOptions, serverOptions, requestTimeout, nonceProvider);
  }
}

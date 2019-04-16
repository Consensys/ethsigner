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

import tech.pegasys.ethsigner.requesthandler.sendtransaction.RawTransactionConverter;
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
  private RawTransactionConverter transactionConverter;

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

  public RunnerBuilder setTransactionConverter(final RawTransactionConverter transactionConverter) {
    this.transactionConverter = transactionConverter;
    return this;
  }

  public Runner build() {
    if (serialiser == null) {
      LOG.error("Unable to construct Runner, serialiser is unset.");
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

    if (transactionConverter == null) {
      LOG.error("Unable to construct Runner, transaction transactionConverter is unset.");
      return null;
    }

    if (requestTimeout == null) {
      LOG.error("Unable to construct Runner, requestTimeout is unset.");
      return null;
    }
    return new Runner(
        serialiser, clientOptions, serverOptions, requestTimeout, transactionConverter);
  }
}

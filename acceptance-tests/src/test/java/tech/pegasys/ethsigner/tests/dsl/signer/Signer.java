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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import tech.pegasys.ethsigner.tests.EthSignerProcessRunner;
import tech.pegasys.ethsigner.tests.dsl.Accounts;
import tech.pegasys.ethsigner.tests.dsl.Contracts;
import tech.pegasys.ethsigner.tests.dsl.Eth;
import tech.pegasys.ethsigner.tests.dsl.Transactions;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

public class Signer {

  private static final Logger LOG = LogManager.getLogger();

  private final Accounts accounts;
  private final Contracts contracts;
  private final EthSignerProcessRunner runner;
  private final Transactions transactions;
  private final Web3j jsonRpc;

  public Signer(final SignerConfiguration signerConfig, final NodeConfiguration nodeConfig) {

    LOG.info("EthSigner Web3j service targeting: : " + signerConfig.url());

    this.runner = new EthSignerProcessRunner(signerConfig, nodeConfig);
    this.jsonRpc =
        new JsonRpc2_0Web3j(
            new HttpService(signerConfig.url()),
            signerConfig.pollingInterval().toMillis(),
            Async.defaultExecutorService());

    final Eth eth = new Eth(jsonRpc);
    this.transactions = new Transactions(eth);
    this.contracts = new Contracts(eth, jsonRpc);
    this.accounts = new Accounts(jsonRpc);
  }

  public void start() {
    LOG.info("Starting EthSigner");
    runner.start("EthSigner");
  }

  public void shutdown() {
    LOG.info("Shutting down EthSigner");
    runner.shutdown();
  }

  public Transactions transactions() {
    return transactions;
  }

  public Contracts contracts() {
    return contracts;
  }

  public Accounts accounts() {
    return accounts;
  }

  public void awaitStartupCompletion() {
    LOG.info("Waiting for Signer to become responsive...");
    waitFor(() -> assertThat(jsonRpc.ethBlockNumber().send().hasError()).isFalse());
    LOG.info("Signer is now responsive");
  }
}

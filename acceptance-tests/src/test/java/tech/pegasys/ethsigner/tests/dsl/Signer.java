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
package tech.pegasys.ethsigner.tests.dsl;

import tech.pegasys.ethsigner.tests.EthSignerProcessRunner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

public class Signer {

  private static final Logger LOG = LogManager.getLogger();

  // TODO this to go somewhere
  private static final String LOCALHOST = "127.0.0.1";

  private final Web3j jsonRpc;

  private EthSignerProcessRunner runner;

  public Signer() {

    runner = new EthSignerProcessRunner();

    jsonRpc =
        new JsonRpc2_0Web3j(
            new HttpService("http://" + LOCALHOST + ":" + 9945),
            2000,
            Async.defaultExecutorService());
  }

  public Web3j web3j() {
    return jsonRpc;
  }

  public void start() {
    LOG.info("Starting EthSigner");
    runner.start("EthSigner");
  }

  public void shutdown() {
    LOG.info("Shutting down EthSigner");
    runner.shutdown();
  }
}

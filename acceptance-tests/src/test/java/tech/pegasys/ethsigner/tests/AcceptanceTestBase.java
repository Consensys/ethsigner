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
package tech.pegasys.ethsigner.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import tech.pegasys.ethsigner.tests.dsl.Node;
import tech.pegasys.ethsigner.tests.dsl.PantheonNode;
import tech.pegasys.ethsigner.tests.dsl.Signer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.web3j.protocol.Web3j;

public class AcceptanceTestBase {

  private static final Logger LOG = LogManager.getLogger();

  private static Node ethNode;
  private static Signer ethSigner;

  @BeforeClass
  public static void setUpBase() {
    Runtime.getRuntime().addShutdownHook(new Thread(AcceptanceTestBase::tearDownBase));

    ethSigner = new Signer();
    ethNode = new PantheonNode();

    ethNode.start();
    ethSigner.start();

    awaitPantheonStartup();
    awaitEthSignerStartup();
  }

  @AfterClass
  public static void tearDownBase() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }

  protected Web3j ethSigner() {
    return ethSigner.web3j();
  }

  protected Web3j ethNode() {
    return ethNode.web3j();
  }

  private static void awaitEthSignerStartup() {
    LOG.info("Waiting for EthSigner to become responsive...");
    waitFor(() -> assertThat(ethSigner.web3j().ethBlockNumber().send().hasError()).isFalse());
    LOG.info("EthSigner is now responsive");
  }

  private static void awaitPantheonStartup() {
    LOG.info("Waiting for Pantheon to become responsive...");
    waitFor(() -> assertThat(ethNode.web3j().ethBlockNumber().send().hasError()).isFalse());
    LOG.info("Pantheon is now responsive");
  }
}

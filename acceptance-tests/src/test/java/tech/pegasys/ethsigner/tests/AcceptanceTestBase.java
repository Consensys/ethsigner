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

import tech.pegasys.ethsigner.tests.dsl.ConfigurationFactory;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.PantheonNode;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class AcceptanceTestBase {

  private static Node ethNode;
  private static Signer ethSigner;

  protected Signer ethSigner() {
    return ethSigner;
  }

  protected Node ethNode() {
    return ethNode;
  }

  @BeforeClass
  public static void setUpBase() {
    Runtime.getRuntime().addShutdownHook(new Thread(AcceptanceTestBase::tearDownBase));

    final ConfigurationFactory config = new ConfigurationFactory();
    ethSigner = new Signer(config.signer(), config.node());
    ethNode = new PantheonNode(config.docker(), config.node());

    ethNode.start();
    ethSigner.start();

    ethNode.awaitStartupCompletion();
    ethSigner.awaitStartupCompletion();
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
}

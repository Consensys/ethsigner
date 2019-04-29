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
package tech.pegasys.ethsigner.tests.signing;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.PantheonNode;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;

import com.github.dockerjava.api.DockerClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReplayProtectionAcceptanceTest {

  private static Node ethNode;
  private static Signer ethSigner;

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  @BeforeClass
  public static void setUpOnce() {
    Runtime.getRuntime().addShutdownHook(new Thread(AcceptanceTestBase::tearDownBase));
  }

  @AfterClass
  public static void tearDownOnce() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }

  // TODO these should be elsewhere?
  private void setupEthSigner(
      final SignerConfiguration signerConfig, final NodeConfiguration nodeConfig) {
    ethSigner = new Signer(signerConfig, nodeConfig);
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  private void setUpEthNode(final NodeConfiguration nodeConfig) {
    final DockerClient docker = new DockerClientFactory().create();
    ethNode = new PantheonNode(docker, nodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();
  }

  @Test
  public void wrongChainId() {
    // TODO value transfer - expecting error

    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();
    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();
  }

  @Test
  public void missingChainId() {
    // TODO value transfer - expecting error
  }

  @Test
  public void unecessaryChainId() {
    // TODO value transfer - expecting error
  }
}

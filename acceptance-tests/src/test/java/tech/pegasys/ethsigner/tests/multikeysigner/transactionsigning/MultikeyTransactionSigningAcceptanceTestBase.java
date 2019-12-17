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
package tech.pegasys.ethsigner.tests.multikeysigner.transactionsigning;

import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.node.BesuNode;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.multikeysigner.MultiKeyAcceptanceTestBase;

import java.nio.file.Path;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.AfterAll;

public class MultikeyTransactionSigningAcceptanceTestBase extends MultiKeyAcceptanceTestBase {

  protected static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
  protected static Node ethNode;
  protected static Signer ethSigner;

  protected Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  protected static void setUpBase(final Path tomlDirectory) {
    Runtime.getRuntime()
        .addShutdownHook(new Thread(MultikeyTransactionSigningAcceptanceTestBase::tearDownBase));

    final DockerClient docker = new DockerClientFactory().create();
    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();

    ethNode = new BesuNode(docker, nodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withMultiKeySignerDirectory(tomlDirectory).build();

    ethSigner = new Signer(signerConfig, nodeConfig, ethNode.ports());
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  @AfterAll
  public static void tearDownBase() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }
}

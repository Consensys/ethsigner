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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.PantheonNode;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.hashicorpVault.HashicorpVaultDocker;

import java.math.BigInteger;

import com.github.dockerjava.api.DockerClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;

public class ValueTransferWithHashicorpAcceptanceTest {

  private static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";

  private static Node ethNode;
  private static Signer ethSigner;
  private static HashicorpVaultDocker hashicorpVaultDocker;

  @BeforeClass
  public static void setUpBase() {

    Runtime.getRuntime()
        .addShutdownHook(new Thread(ValueTransferWithHashicorpAcceptanceTest::tearDownBase));

    final DockerClient docker = new DockerClientFactory().create();
    hashicorpVaultDocker = new HashicorpVaultDocker(docker);
    hashicorpVaultDocker.start();
    hashicorpVaultDocker.awaitStartupCompletion();
    final int port = hashicorpVaultDocker.port();

    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();
    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withHashicorpVaultPort(port).build();

    ethNode = new PantheonNode(docker, nodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    ethSigner = new Signer(signerConfig, nodeConfig, ethNode.ports());
    ethSigner.start();
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

    if (hashicorpVaultDocker != null) {
      hashicorpVaultDocker.shutdown();
    }
  }

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  private Signer ethSigner() {
    return ethSigner;
  }

  private Node ethNode() {
    return ethNode;
  }

  @Test
  public void valueTransfer() {
    final BigInteger transferAmountWei =
        Convert.toWei("1.75", Convert.Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = ethNode().accounts().balance(RECIPIENT);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            RECIPIENT,
            transferAmountWei);

    final String hash = ethSigner().transactions().submit(transaction);
    ethNode().transactions().awaitBlockContaining(hash);

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance = ethNode().accounts().balance(RECIPIENT);
    assertThat(actualEndBalance).isEqualTo(expectedEndBalance);
  }
}

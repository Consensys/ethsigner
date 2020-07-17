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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfig;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfigBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeFactory;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.multikeysigner.MultiKeyAcceptanceTestBase;

import java.math.BigInteger;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class MultiKeyTransactionSigningAcceptanceTestBase extends MultiKeyAcceptanceTestBase {

  protected static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
  protected static Node ethNode;
  protected static Signer ethSigner;

  protected Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  protected static void setup(final Path tomlDirectory) {
    Runtime.getRuntime()
        .addShutdownHook(new Thread(MultiKeyTransactionSigningAcceptanceTestBase::tearDownBase));

    final BesuNodeConfig besuNodeConfig = BesuNodeConfigBuilder.aBesuNodeConfig().build();
    ethNode = BesuNodeFactory.create(besuNodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withMultiKeySignerDirectory(tomlDirectory).build();

    ethSigner = new Signer(signerConfig, besuNodeConfig.getHostName(), ethNode.ports());
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  void performTransaction() {
    final BigInteger transferAmountWei = Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact();

    final BigInteger startBalance = ethNode.accounts().balance(RECIPIENT);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            RECIPIENT,
            transferAmountWei);

    final String hash = ethSigner.transactions().submit(transaction);
    ethNode.transactions().awaitBlockContaining(hash);

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance = ethNode.accounts().balance(RECIPIENT);
    assertThat(actualEndBalance).isEqualTo(expectedEndBalance);
  }

  @AfterAll
  public static synchronized void tearDownBase() {
    if (ethNode != null) {
      ethNode.shutdown();
      ethNode = null;
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
      ethSigner = null;
    }
  }
}

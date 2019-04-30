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
import static tech.pegasys.ethsigner.tests.dsl.utils.ProgrammaticLogLevel.setLogLevelToDebug;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
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

import java.io.IOException;
import java.math.BigInteger;

import com.github.dockerjava.api.DockerClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class ReplayProtectionAcceptanceTest {

  private static final DockerClient DOCKER = new DockerClientFactory().create();
  private static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
  private static final BigInteger TRANSFER_AMOUNT_WEI =
      Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact();

  private Node ethNode;
  private Signer ethSigner;

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  @Before
  public void setUp() {
    Runtime.getRuntime().addShutdownHook(new Thread(AcceptanceTestBase::tearDownBase));
    setLogLevelToDebug();
  }

  @After
  public void tearDown() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }

  private void setUp(final String genesis) {
    final NodeConfiguration nodeConfig =
        new NodeConfigurationBuilder().withGenesis(genesis).build();
    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();

    ethSigner = new Signer(signerConfig, nodeConfig);
    ethNode = new PantheonNode(DOCKER, nodeConfig);

    ethNode.start();
    ethSigner.start();

    ethNode.awaitStartupCompletion();
    ethSigner.awaitStartupCompletion();
  }

  @Test
  public void wrongChainId() throws IOException {
    setUp("eth_hash_4404.json");

    final JsonRpcErrorResponse error =
        ethSigner
            .transactions()
            .submitExceptional(
                Transaction.createEtherTransaction(
                    richBenefactor().address(),
                    richBenefactor().getNextNonceAndIncrement(),
                    GAS_PRICE,
                    INTRINSIC_GAS,
                    RECIPIENT,
                    TRANSFER_AMOUNT_WEI));

    assertThat(error.getError()).isEqualTo(JsonRpcError.INVALID_PARAMS);
  }

  @Test
  public void unnecessaryChainId() throws IOException {
    setUp("eth_hash_2018_no_replay_protection.json");

    final JsonRpcErrorResponse error =
        ethSigner
            .transactions()
            .submitExceptional(
                Transaction.createEtherTransaction(
                    richBenefactor().address(),
                    richBenefactor().getNextNonceAndIncrement(),
                    GAS_PRICE,
                    INTRINSIC_GAS,
                    RECIPIENT,
                    TRANSFER_AMOUNT_WEI));

    assertThat(error.getError()).isEqualTo(JsonRpcError.INVALID_PARAMS);
  }
}

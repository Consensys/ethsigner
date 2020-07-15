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
package tech.pegasys.ethsigner.tests.timeouts;

import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfig;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class ReadTimeoutAcceptanceTest {

  private static final Logger LOG = LogManager.getLogger();
  private static final int DYNAMICALLY_ASSIGN_PORT = 0;

  private Signer ethSigner;
  private ServerSocket unresponsiveSocketA;
  private ServerSocket unresponsiveSocketB;

  @BeforeEach
  public void setUp() throws IOException {
    unresponsiveSocketA = new ServerSocket(DYNAMICALLY_ASSIGN_PORT);
    unresponsiveSocketB = new ServerSocket(DYNAMICALLY_ASSIGN_PORT);

    final BesuNodePorts besuNodePorts =
        new BesuNodePorts(unresponsiveSocketA.getLocalPort(), unresponsiveSocketB.getLocalPort());
    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();

    ethSigner = new Signer(signerConfig, BesuNodeConfig.DEFAULT_HOST, besuNodePorts);
    ethSigner.start();
    ethSigner.awaitStartupCompletion();

    Runtime.getRuntime().addShutdownHook(new Thread((this::tearDown)));
  }

  @AfterEach
  public synchronized void tearDown() {
    if (ethSigner != null) {
      ethSigner.shutdown();
    }

    close(unresponsiveSocketA);
    close(unresponsiveSocketB);
  }

  private void close(final ServerSocket socket) {
    try {
      if (!socket.isClosed()) {
        socket.close();
      }
    } catch (final IOException e) {
      LOG.warn("Problem closing unresponsive socket {}", socket.getInetAddress(), e);
    }
  }

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  @Test
  public void submittingTransactionReturnsAGatewayTimeoutError() {
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("15.5", Unit.ETHER).toBigIntegerExact();

    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonceAndIncrement(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);
    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner.transactions().submitExceptional(transaction);
    assertThat(signerResponse.status()).isEqualTo(GATEWAY_TIMEOUT);
    assertThat(signerResponse.jsonRpc().getError())
        .isEqualTo(CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT);
  }

  @Test
  public void submittingTransactionWithoutNonceReturnsAGatewayTimeoutError() {
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("15.5", Unit.ETHER).toBigIntegerExact();

    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);
    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner.transactions().submitExceptional(transaction);
    assertThat(signerResponse.status()).isEqualTo(GATEWAY_TIMEOUT);
    assertThat(signerResponse.jsonRpc().getError())
        .isEqualTo(CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT);
  }
}

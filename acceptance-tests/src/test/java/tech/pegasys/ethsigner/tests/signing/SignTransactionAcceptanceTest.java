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
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.dsl.Accounts;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class SignTransactionAcceptanceTest extends AcceptanceTestBase {

  /** Number of GAS units that the transaction will cost. */
  private static final BigInteger INTRINSIC_GAS = BigInteger.valueOf(21000);

  @Test
  public void valueTransfer() throws IOException {

    final BigInteger nonce = BigInteger.ZERO;
    final BigInteger gasPrice = BigInteger.valueOf(10000000000000L);
    final BigDecimal transferAmount = new BigDecimal(15.5);
    final Unit transferUnit = Unit.ETHER;
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";

    final BigInteger transferAmountWei = new BigInteger("15500000000000000000");

    // TODO starting balance - refactor into private methods

    // TODO logging of calls (for test setup / querying)

    final BigInteger startBalance =
        ethNode()
            .ethGetBalance(
                recipient,
                DefaultBlockParameter.valueOf(ethNode().ethBlockNumber().send().getBlockNumber()))
            .send()
            .getBalance();

    final Transaction transaction =
        Transaction.createEtherTransaction(
            Accounts.GENESIS_ACCOUNT_ONE_PUBLIC_KEY,
            nonce,
            gasPrice,
            INTRINSIC_GAS,
            recipient,
            Convert.toWei(transferAmount, transferUnit).toBigIntegerExact());

    final EthSendTransaction response = ethSigner().ethSendTransaction(transaction).send();

    assertThat(response.getTransactionHash()).isNotEmpty();
    assertThat(response.getError()).isNull();

    final String hash = response.getTransactionHash();

    waitFor(
        () ->
            assertThat(
                    ethNode()
                        .ethGetTransactionReceipt(hash)
                        .send()
                        .getTransactionReceipt()
                        .isPresent())
                .isTrue());

    final EthGetTransactionReceipt r = ethNode().ethGetTransactionReceipt(hash).send();

    // TODO use the ifPresent()
    final BigInteger settledBlock = r.getTransactionReceipt().get().getBlockNumber();

    final EthGetBalance endBalance =
        ethNode().ethGetBalance(recipient, DefaultBlockParameter.valueOf(settledBlock)).send();
    assertThat(endBalance.getBalance())
        .isCloseTo(startBalance.add(transferAmountWei), Offset.offset(BigInteger.ONE));
  }

  @Test
  public void contract() {

    // TODO contract

    // TODO verify deployment on node
  }
}

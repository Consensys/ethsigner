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
import static tech.pegasys.ethsigner.tests.dsl.Accounts.RICH_BENEFACTOR;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;

import java.io.IOException;
import java.math.BigInteger;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class SignTransactionAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void valueTransfer() throws IOException {
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("15.5", Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = balance(recipient);

    final Transaction transaction =
        Transaction.createEtherTransaction(
            RICH_BENEFACTOR.address(),
            RICH_BENEFACTOR.getNextNonceAndIncrement(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);

    final String hash = submitTransaction(transaction);

    awaitTransactionInclusion(hash);

    final BigInteger endBalance = balance(recipient, blockContains(hash));

    assertThat(endBalance)
        .isCloseTo(startBalance.add(transferAmountWei), Offset.offset(BigInteger.ONE));
  }

  @Test
  public void contract() {

    // TODO contract

    // TODO verify deployment on node
  }

  // TODO refactor these elsewhere
  private BigInteger blockContains(final String hash) throws IOException {
    final EthGetTransactionReceipt transactionReceipt =
        ethNode().ethGetTransactionReceipt(hash).send();

    assertThat(transactionReceipt).isNotNull();
    assertThat(transactionReceipt.getTransactionReceipt()).isNotNull();
    assertThat(transactionReceipt.getTransactionReceipt().isPresent()).isTrue();

    return transactionReceipt.getTransactionReceipt().get().getBlockNumber();
  }

  private String submitTransaction(final Transaction transaction) throws IOException {
    final EthSendTransaction response = ethSigner().ethSendTransaction(transaction).send();

    assertThat(response.getTransactionHash()).isNotEmpty();
    assertThat(response.getError()).isNull();

    return response.getTransactionHash();
  }

  private void awaitTransactionInclusion(final String hash) {
    waitFor(
        () ->
            assertThat(
                    ethNode()
                        .ethGetTransactionReceipt(hash)
                        .send()
                        .getTransactionReceipt()
                        .isPresent())
                .isTrue());
  }

  private BigInteger balance(final String account) throws IOException {
    return ethNode()
        .ethGetBalance(
            account,
            DefaultBlockParameter.valueOf(ethNode().ethBlockNumber().send().getBlockNumber()))
        .send()
        .getBalance();
  }

  private BigInteger balance(final String account, final BigInteger atBlock) throws IOException {
    return ethNode()
        .ethGetBalance(account, DefaultBlockParameter.valueOf(atBlock))
        .send()
        .getBalance();
  }
}

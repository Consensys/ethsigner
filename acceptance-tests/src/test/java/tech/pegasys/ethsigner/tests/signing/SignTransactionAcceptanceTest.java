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
import static tech.pegasys.ethsigner.tests.dsl.Accounts.RICH_BENEFACTOR;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;

import java.io.IOException;
import java.math.BigInteger;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class SignTransactionAcceptanceTest extends AcceptanceTestBase {

  public static final int NO_OF_TRANSACTIONS = 50;

  @Test
  public void valueTransfer() throws IOException {
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("15.5", Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = ethNode().accounts().balance(recipient);

    final Transaction transaction =
        Transaction.createEtherTransaction(
            RICH_BENEFACTOR.address(),
            RICH_BENEFACTOR.getNextNonceAndIncrement(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);

    final String hash = ethSigner().transactions().submit(transaction);

    ethNode().transactions().awaitBlockContaining(hash);

    final BigInteger endBalance =
        ethNode().accounts().balance(recipient, ethNode().transactions().blockContaining(hash));

    assertThat(endBalance)
        .isCloseTo(startBalance.add(transferAmountWei), Offset.offset(BigInteger.ONE));
  }

  @Test
  public void multipleValueTransfers() throws IOException {
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("1", Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = ethNode().accounts().balance(recipient);

    final Transaction transaction =
        Transaction.createEtherTransaction(
            RICH_BENEFACTOR.address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);

    String hash = null;
    for (int i = 0; i < NO_OF_TRANSACTIONS; i++) {
      hash = ethSigner().transactions().submit(transaction);
    }

    ethNode().transactions().awaitBlockContaining(hash);

    final BigInteger endBalance =
        ethNode().accounts().balance(recipient, ethNode().transactions().blockContaining(hash));

    BigInteger numberOfTransactions = BigInteger.valueOf(NO_OF_TRANSACTIONS);
    assertThat(endBalance)
        .isCloseTo(
            startBalance.add(transferAmountWei.multiply(numberOfTransactions)),
            Offset.offset(BigInteger.ONE));
  }

  @Test
  public void valueTransferNonceTooLow() throws IOException {

    valueTransfer(); // call this test to increment the nonce

    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("15.5", Unit.ETHER).toBigIntegerExact();

    final Transaction transaction =
        Transaction.createEtherTransaction(
            RICH_BENEFACTOR.address(),
            RICH_BENEFACTOR
                .getNextNonceAndIncrement()
                .subtract(BigInteger.ONE), // same nonce as last tx
            GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);

    try {
      ethSigner().transactions().submit(transaction);
    } catch (ClientConnectionException e) {
      String message = e.getMessage();
      if (message.indexOf("Nonce too low") != -1) {
        return; // success
      } else {
        Assertions.fail(
            "Message from ClientConnectionException: \nSTART_OF_MESSSAGE\n"
                + message
                + "\nEND_OF_MESSAGE\ndoes not contain the 'Nonce too low'");
      }
    }
    Assertions.fail("Expected ClientConnectionException not thrown");
  }

  @Test
  public void contract() {

    // TODO contract

    // TODO verify deployment on node
  }
}

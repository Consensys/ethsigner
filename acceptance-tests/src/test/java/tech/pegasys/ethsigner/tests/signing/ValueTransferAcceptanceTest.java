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

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.dsl.Account;

import java.io.IOException;
import java.math.BigInteger;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class ValueTransferAcceptanceTest extends AcceptanceTestBase {

  private static final Offset<BigInteger> NO_OFFSET = Offset.offset(BigInteger.ZERO);

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

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance =
        ethNode().accounts().balance(recipient, ethNode().transactions().blockContaining(hash));

    assertThat(actualEndBalance).isCloseTo(expectedEndBalance, NO_OFFSET);
  }

  @Test
  public void valueTransferFromAccountWithInsufficientFunds() throws IOException {
    final String recipientAddress = "0x1b11ba11ca11bb11aa11bc11be11ac11ca11da11";
    final BigInteger senderStartBalance = ethNode().accounts().balance(RICH_BENEFACTOR);
    final BigInteger recipientStartBalance = ethNode().accounts().balance(recipientAddress);
    final BigInteger transferAmountWei = senderStartBalance.multiply(BigInteger.TEN);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            RICH_BENEFACTOR.address(),
            RICH_BENEFACTOR.getNextNonce(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipientAddress,
            transferAmountWei);

    final JsonRpcErrorResponse error = ethSigner().transactions().submitExceptional(transaction);

    assertThat(error.getError()).isEqualTo(JsonRpcError.TRANSACTION_UPFRONT_COST_EXCEEDS_BALANCE);

    final BigInteger senderEndBalance = ethNode().accounts().balance(RICH_BENEFACTOR);
    final BigInteger recipientEndBalance = ethNode().accounts().balance(recipientAddress);

    assertThat(senderEndBalance).isCloseTo(senderStartBalance, NO_OFFSET);
    assertThat(recipientEndBalance).isCloseTo(recipientStartBalance, NO_OFFSET);
  }

  @Test
  public void senderIsNotUnlockedAccount() throws IOException {
    final Account sender = new Account("0x223b55228fb22b89f2216b7222e5522b8222bd22");
    final String recipientAddress = "0x1b22ba22ca22bb22aa22bc22be22ac22ca22da22";
    final BigInteger senderStartBalance = ethNode().accounts().balance(sender);
    final BigInteger recipientStartBalance = ethNode().accounts().balance(recipientAddress);
    final BigInteger transferAmountWei = senderStartBalance;
    final Transaction transaction =
        Transaction.createEtherTransaction(
            sender.address(),
            sender.getNextNonce(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipientAddress,
            transferAmountWei);

    final JsonRpcErrorResponse error = ethSigner().transactions().submitExceptional(transaction);

    assertThat(error.getError()).isEqualTo(JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);

    final BigInteger senderEndBalance = ethNode().accounts().balance(sender);
    final BigInteger recipientEndBalance = ethNode().accounts().balance(recipientAddress);

    assertThat(senderEndBalance).isCloseTo(senderStartBalance, NO_OFFSET);
    assertThat(recipientEndBalance).isCloseTo(recipientStartBalance, NO_OFFSET);
  }
}

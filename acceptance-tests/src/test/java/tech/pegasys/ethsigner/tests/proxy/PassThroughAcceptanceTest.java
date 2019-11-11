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
package tech.pegasys.ethsigner.tests.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;

public class PassThroughAcceptanceTest extends AcceptanceTestBase {

  private static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
  private static final String TRANSFER_AMOUNT_ETHER = "10";
  private static final BigInteger TRANSFER_AMOUNT_WEI =
      Convert.toWei(TRANSFER_AMOUNT_ETHER, Convert.Unit.ETHER).toBigIntegerExact();

  @Test
  public void ethGetTransactionCountReturnCorrectNumber() {
    final BigInteger web3ProviderTransactionCount =
        ethNode().transactions().count(richBenefactor().address());
    BigInteger ethSignerTransactionCount =
        ethSigner().transactions().count(richBenefactor().address());

    assertThat(web3ProviderTransactionCount).isEqualTo(ethSignerTransactionCount);

    submitTransactionAndWaitForBlock(); // should increase the transaction count by one.

    ethSignerTransactionCount = ethSigner().transactions().count(richBenefactor().address());

    assertThat(web3ProviderTransactionCount.add(BigInteger.ONE))
        .isEqualTo(ethSignerTransactionCount);
  }

  @Test
  public void ethBalanceRequestReturnsCorrectBalance() {
    final BigInteger ethSignerStartBalance = ethSigner().accounts().balance(RECIPIENT);

    submitTransactionAndWaitForBlock();

    final BigInteger ethSignerEndBalance = ethSigner().accounts().balance(RECIPIENT);

    assertThat(ethSignerEndBalance)
        .isEqualByComparingTo(ethSignerStartBalance.add(TRANSFER_AMOUNT_WEI));

    final BigInteger web3ProviderBalance = ethNode().accounts().balance(RECIPIENT);

    assertThat(web3ProviderBalance).isEqualTo(ethSignerEndBalance);
  }

  void submitTransactionAndWaitForBlock() {

    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            RECIPIENT,
            TRANSFER_AMOUNT_WEI);

    final String hash = ethSigner().transactions().submit(transaction);
    ethNode().transactions().awaitBlockContaining(hash);
  }
}

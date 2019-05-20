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

import org.junit.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;

public class PassThroughAcceptanceTest extends AcceptanceTestBase {

  private static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
  private static final String TRANSFER_AMOUNT_ETHER = "10";
  private static final BigInteger TRANSFER_AMOUNT_WEI =
      Convert.toWei(TRANSFER_AMOUNT_ETHER, Convert.Unit.ETHER).toBigIntegerExact();

  @Test
  public void ethGetTransactionCountReturnCorrectNumber() {
    final BigInteger startTransactionCount =
        ethNode().transactions().count(richBenefactor().address());
    submitTransactionAndWaitForBlock();

    final BigInteger endTransactionCount =
        ethNode().transactions().count(richBenefactor().address());

    assertThat(startTransactionCount.add(BigInteger.ONE)).isEqualTo(endTransactionCount);
  }

  @Test
  public void mySimpleTest() {
    final BigInteger endTransactionCount =
        ethSigner().transactions().count(richBenefactor().address());
  }

  @Test
  public void ethBalanceRequesReturnsCorrectBalance() {
    final BigInteger startBalance = ethNode().accounts().balance(RECIPIENT);
    submitTransactionAndWaitForBlock();

    final BigInteger endBalance = ethNode().accounts().balance(RECIPIENT);

    assertThat(endBalance).isEqualByComparingTo(startBalance.add(TRANSFER_AMOUNT_WEI));
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

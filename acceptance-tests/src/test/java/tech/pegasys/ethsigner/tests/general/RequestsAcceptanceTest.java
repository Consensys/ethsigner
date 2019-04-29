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
package tech.pegasys.ethsigner.tests.general;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.dsl.Accounts.RICH_BENEFACTOR;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;

import java.io.IOException;
import java.math.BigInteger;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Convert;
import tech.pegasys.ethsigner.tests.dsl.Transactions;

public class RequestsAcceptanceTest extends AcceptanceTestBase {

  static final String TRANFER_AMOUNT_ETHER = "10";
  static final BigInteger TRANSFER_AMOUNT_WEI =
      Convert.toWei(TRANFER_AMOUNT_ETHER, Convert.Unit.ETHER).toBigIntegerExact();

  @Test
  public void ethAccountsReturnsAccountForPrivKey() throws Exception {

    final EthAccounts ethAccountsRequest = ethSigner().jsonRpc().ethAccounts().send();

    assertThat(ethAccountsRequest.getAccounts().size() == 1)
        .isTrue(); // we only have one private key
    final String account = ethAccountsRequest.getAccounts().get(0);
    assertThat(ethNode().accounts().balance(account)).isNotNull();
  }

  @Test
  public void ethGetTransactionCountReturnCorrectNumber() throws Exception {
    final Request<?, EthGetTransactionCount> ethGetTransactionCountRequest =
        ethSigner()
            .jsonRpc()
            .ethGetTransactionCount(RICH_BENEFACTOR.address(), DefaultBlockParameterName.LATEST);

    submitTransactionAndWaitForBlock();

    EthGetTransactionCount ethGetTransactionCount = ethGetTransactionCountRequest.send();
    final BigInteger startTransactionCount = ethGetTransactionCount.getTransactionCount();

    submitTransactionAndWaitForBlock();

    ethGetTransactionCount = ethGetTransactionCountRequest.send();
    final BigInteger endTransactionCount = ethGetTransactionCount.getTransactionCount();

    assertThat(startTransactionCount.add(BigInteger.ONE).equals(endTransactionCount)).isTrue();
  }

  @Test
  public void ethBalanceRequesReturnsCorrectBalance() throws Exception {
    final Request<?, EthGetBalance> ethGetBalanceRequest =
        ethSigner().jsonRpc().ethGetBalance(RECIPIENT, DefaultBlockParameterName.LATEST);

    submitTransactionAndWaitForBlock();

    EthGetBalance ethGetBalance = ethGetBalanceRequest.send();
    final BigInteger startBalance = ethGetBalance.getBalance();

    submitTransactionAndWaitForBlock();

    ethGetBalance = ethGetBalanceRequest.send();
    final BigInteger endBalance = ethGetBalance.getBalance();

    assertThat(endBalance)
        .isCloseTo(startBalance.add(TRANSFER_AMOUNT_WEI), Offset.offset(BigInteger.ONE));
  }

  void submitTransactionAndWaitForBlock() throws IOException {

    final Transaction transaction =
        Transaction.createEtherTransaction(
            RICH_BENEFACTOR.address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            RECIPIENT,
            TRANSFER_AMOUNT_WEI);

    final String hash = ethSigner().transactions().submit(transaction);

    ethNode().transactions().awaitBlockContaining(hash);
  }
}

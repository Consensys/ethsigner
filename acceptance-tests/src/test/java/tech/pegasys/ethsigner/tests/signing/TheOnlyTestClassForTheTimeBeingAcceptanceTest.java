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
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_LIMIT;
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;
import static tech.pegasys.ethsigner.tests.dsl.utils.Hex.hex;
import static tech.pegasys.ethsigner.tests.dsl.utils.Offset.NO_OFFSET;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.Gas;
import tech.pegasys.ethsigner.tests.signing.contract.generated.SimpleStorage;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class TheOnlyTestClassForTheTimeBeingAcceptanceTest extends AcceptanceTestBase {

  private static final String SIMPLE_STORAGE_BINARY = SimpleStorage.BINARY;
  private static final String SIMPLE_STORAGE_GET = "0x6d4ce63c";
  private static final String SIMPLE_STORAGE_SET_7 =
      "0x60fe47b10000000000000000000000000000000000000000000000000000000000000007";

  @Test
  public void smartContractAcceptanceTest_DeployContract() throws IOException {
    final Transaction contract =
        Transaction.createContractTransaction(
            richBenefactor().address(),
            richBenefactor().getNextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            BigInteger.ZERO,
            SIMPLE_STORAGE_BINARY);

    final String hash = ethSigner().contracts().submit(contract);
    ethNode().contracts().awaitBlockContaining(hash);

    final String address = ethNode().contracts().address(hash);
    final String code = ethNode().contracts().code(address);
    assertThat(code)
        .isEqualTo(
            "0x60806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166360fe47b18114604d5780636d4ce63c146075575b600080fd5b348015605857600080fd5b50607360048036036020811015606d57600080fd5b50356099565b005b348015608057600080fd5b506087609e565b60408051918252519081900360200190f35b600055565b6000549056fea165627a7a72305820cb1d0935d14b589300b12fcd0ab849a7e9019c81da24d6daa4f6b2f003d1b0180029");
  }

  @Test
  public void smartContractAcceptanceTest_InvokeContract() throws IOException {
    final Transaction contract =
        Transaction.createContractTransaction(
            richBenefactor().address(),
            richBenefactor().getNextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            BigInteger.ZERO,
            SIMPLE_STORAGE_BINARY);

    final String hash = ethSigner().contracts().submit(contract);
    ethNode().contracts().awaitBlockContaining(hash);

    final String contractAddress = ethNode().contracts().address(hash);
    final Transaction valueBeforeChange =
        Transaction.createEthCallTransaction(
            richBenefactor().address(), contractAddress, SIMPLE_STORAGE_GET);
    final BigInteger startingValue = hex(ethSigner().contracts().call(valueBeforeChange));
    final Transaction changeValue =
        Transaction.createFunctionCallTransaction(
            richBenefactor().address(),
            richBenefactor().getNextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            contractAddress,
            SIMPLE_STORAGE_SET_7);

    final String valueUpdate = ethSigner().contracts().submit(changeValue);
    ethNode().contracts().awaitBlockContaining(valueUpdate);

    final Transaction valueAfterChange =
        Transaction.createEthCallTransaction(
            richBenefactor().address(), contractAddress, SIMPLE_STORAGE_GET);
    final BigInteger endValue = hex(ethSigner().contracts().call(valueAfterChange));
    assertThat(endValue).isCloseTo(startingValue.add(BigInteger.valueOf(7)), NO_OFFSET);
  }

  @Test
  public void valueTransferAcceptanceTest_ValueTransfer() throws IOException {
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = ethNode().accounts().balance(recipient);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            richBenefactor().getNextNonceAndIncrement(),
            Gas.GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);

    final String hash = ethSigner().transactions().submit(transaction);
    ethNode().transactions().awaitBlockContaining(hash);

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance = ethNode().accounts().balance(recipient);
    assertThat(actualEndBalance).isCloseTo(expectedEndBalance, NO_OFFSET);
  }

  @Test
  public void valueTransferAcceptanceTest_ValueTransferFromAccountWithInsufficientFunds()
      throws IOException {
    final String recipientAddress = "0x1b11ba11ca11bb11aa11bc11be11ac11ca11da11";
    final BigInteger senderStartBalance = ethNode().accounts().balance(richBenefactor());
    final BigInteger recipientStartBalance = ethNode().accounts().balance(recipientAddress);
    final BigInteger transferAmountWei = senderStartBalance.multiply(BigInteger.TEN);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            richBenefactor().getNextNonce(),
            Gas.GAS_PRICE,
            INTRINSIC_GAS,
            recipientAddress,
            transferAmountWei);

    final JsonRpcErrorResponse error = ethSigner().transactions().submitExceptional(transaction);
    assertThat(error.getError()).isEqualTo(JsonRpcError.TRANSACTION_UPFRONT_COST_EXCEEDS_BALANCE);

    final BigInteger senderEndBalance = ethNode().accounts().balance(richBenefactor());
    final BigInteger recipientEndBalance = ethNode().accounts().balance(recipientAddress);
    assertThat(senderEndBalance).isCloseTo(senderStartBalance, NO_OFFSET);
    assertThat(recipientEndBalance).isCloseTo(recipientStartBalance, NO_OFFSET);
  }

  @Test
  public void valueTransferAcceptanceTest_SenderIsNotUnlockedAccount() throws IOException {
    final Account sender = new Account("0x223b55228fb22b89f2216b7222e5522b8222bd22");
    final String recipientAddress = "0x1b22ba22ca22bb22aa22bc22be22ac22ca22da22";
    final BigInteger senderStartBalance = ethNode().accounts().balance(sender);
    final BigInteger recipientStartBalance = ethNode().accounts().balance(recipientAddress);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            sender.address(),
            sender.getNextNonce(),
            Gas.GAS_PRICE,
            INTRINSIC_GAS,
            recipientAddress,
            senderStartBalance);

    final JsonRpcErrorResponse error = ethSigner().transactions().submitExceptional(transaction);
    assertThat(error.getError()).isEqualTo(JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);

    final BigInteger senderEndBalance = ethNode().accounts().balance(sender);
    final BigInteger recipientEndBalance = ethNode().accounts().balance(recipientAddress);
    assertThat(senderEndBalance).isCloseTo(senderStartBalance, NO_OFFSET);
    assertThat(recipientEndBalance).isCloseTo(recipientStartBalance, NO_OFFSET);
  }
}

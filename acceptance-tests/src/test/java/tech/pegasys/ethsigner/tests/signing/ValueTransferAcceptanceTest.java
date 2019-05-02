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

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.TRANSACTION_UPFRONT_COST_EXCEEDS_BALANCE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.Test;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class ValueTransferAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void valueTransfer() throws IOException {
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = ethNode().accounts().balance(recipient);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonceAndIncrement(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipient,
            transferAmountWei);

    final String hash = ethSigner().transactions().submit(transaction);
    ethNode().transactions().awaitBlockContaining(hash);

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance = ethNode().accounts().balance(recipient);
    assertThat(actualEndBalance).isEqualTo(expectedEndBalance);
  }

  @Test
  public void valueTransferFromAccountWithInsufficientFunds() throws IOException {
    final String recipientAddress = "0x1b11ba11ca11bb11aa11bc11be11ac11ca11da11";
    final BigInteger senderStartBalance = ethNode().accounts().balance(richBenefactor());
    final BigInteger recipientStartBalance = ethNode().accounts().balance(recipientAddress);
    final BigInteger transferAmountWei = senderStartBalance.add(BigInteger.ONE);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonce(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipientAddress,
            transferAmountWei);

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner().transactions().submitExceptional(transaction);
    assertThat(signerResponse.status()).isEqualTo(BAD_REQUEST);
    assertThat(signerResponse.jsonRpc().getError())
        .isEqualTo(TRANSACTION_UPFRONT_COST_EXCEEDS_BALANCE);

    final BigInteger senderEndBalance = ethNode().accounts().balance(richBenefactor());
    final BigInteger recipientEndBalance = ethNode().accounts().balance(recipientAddress);
    assertThat(senderEndBalance).isEqualTo(senderStartBalance);
    assertThat(recipientEndBalance).isEqualTo(recipientStartBalance);
  }

  @Test
  public void senderIsNotUnlockedAccount() throws IOException {
    final Account sender = new Account("0x223b55228fb22b89f2216b7222e5522b8222bd22");
    final String recipientAddress = "0x1b22ba22ca22bb22aa22bc22be22ac22ca22da22";
    final BigInteger senderStartBalance = ethNode().accounts().balance(sender);
    final BigInteger recipientStartBalance = ethNode().accounts().balance(recipientAddress);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            sender.address(),
            sender.nextNonce(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipientAddress,
            senderStartBalance);

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner().transactions().submitExceptional(transaction);
    assertThat(signerResponse.status()).isEqualTo(BAD_REQUEST);
    assertThat(signerResponse.jsonRpc().getError())
        .isEqualTo(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);

    final BigInteger senderEndBalance = ethNode().accounts().balance(sender);
    final BigInteger recipientEndBalance = ethNode().accounts().balance(recipientAddress);
    assertThat(senderEndBalance).isEqualTo(senderStartBalance);
    assertThat(recipientEndBalance).isEqualTo(recipientStartBalance);
  }
}

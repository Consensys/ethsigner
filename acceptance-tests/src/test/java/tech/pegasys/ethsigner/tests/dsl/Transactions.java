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
package tech.pegasys.ethsigner.tests.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import java.io.IOException;
import java.math.BigInteger;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class Transactions {

  private final Web3j jsonRpc;

  public Transactions(final Web3j jsonRpc) {
    this.jsonRpc = jsonRpc;
  }

  public String submit(final Transaction transaction) throws IOException {
    final EthSendTransaction response = jsonRpc.ethSendTransaction(transaction).send();

    assertThat(response.getTransactionHash()).isNotEmpty();
    assertThat(response.getError()).isNull();

    return response.getTransactionHash();
  }

  public void awaitBlockContaining(final String hash) {
    waitFor(
        () ->
            assertThat(
                    jsonRpc
                        .ethGetTransactionReceipt(hash)
                        .send()
                        .getTransactionReceipt()
                        .isPresent())
                .isTrue());
  }

  public BigInteger blockContaining(final String hash) throws IOException {
    final EthGetTransactionReceipt transactionReceipt =
        jsonRpc.ethGetTransactionReceipt(hash).send();

    assertThat(transactionReceipt).isNotNull();
    assertThat(transactionReceipt.getTransactionReceipt()).isNotNull();
    assertThat(transactionReceipt.getTransactionReceipt().isPresent()).isTrue();

    return transactionReceipt.getTransactionReceipt().get().getBlockNumber();
  }
}

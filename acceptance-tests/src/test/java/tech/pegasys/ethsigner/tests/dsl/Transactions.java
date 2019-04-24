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
import static org.assertj.core.api.Assertions.fail;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import java.io.IOException;
import java.math.BigInteger;

import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.core.ConditionTimeoutException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.exceptions.ClientConnectionException;

public class Transactions {

  private static final Logger LOG = LogManager.getLogger();

  private final Web3j jsonRpc;

  public Transactions(final Web3j jsonRpc) {
    this.jsonRpc = jsonRpc;
  }

  public String submit(final Transaction transaction) throws IOException {

    // TODO when non-200 (i.e. invalid) exception is thrown: ClientConnectionException
    final EthSendTransaction response = jsonRpc.ethSendTransaction(transaction).send();

    assertThat(response.getTransactionHash()).isNotEmpty();
    assertThat(response.getError()).isNull();

    return response.getTransactionHash();
  }

  public JsonRpcErrorResponse submitExceptional(final Transaction transaction) throws IOException {
    try {
      jsonRpc.ethSendTransaction(transaction).send();
      fail("Expecting exceptional response ");
      return null;
    } catch (final ClientConnectionException e) {
      final String jsonBody = e.getMessage().substring(e.getMessage().indexOf("{"));
      return Json.decodeValue(jsonBody, JsonRpcErrorResponse.class);
    }
  }

  public void awaitBlockContaining(final String hash) {
    try {
      waitFor(
          () ->
              assertThat(
                      jsonRpc
                          .ethGetTransactionReceipt(hash)
                          .send()
                          .getTransactionReceipt()
                          .isPresent())
                  .isTrue());
    } catch (final ConditionTimeoutException e) {
      LOG.error("Timed out waiting for a block containing the transaction receipt hash: " + hash);
    }
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

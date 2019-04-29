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

import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.core.ConditionTimeoutException;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.exceptions.ClientConnectionException;

public class Transactions {

  private static final Logger LOG = LogManager.getLogger();

  private final Eth eth;

  public Transactions(final Eth eth) {
    this.eth = eth;
  }

  public String submit(final Transaction transaction) throws IOException {
    return eth.sendTransaction(transaction);
  }

  public JsonRpcErrorResponse submitExceptional(final Transaction transaction) throws IOException {
    try {
      eth.sendTransaction(transaction);
      fail("Expecting exceptional response ");
      return null;
    } catch (final ClientConnectionException e) {
      final String jsonBody = e.getMessage().substring(e.getMessage().indexOf("{"));
      return Json.decodeValue(jsonBody, JsonRpcErrorResponse.class);
    }
  }

  public void awaitBlockContaining(final String hash) {
    try {
      waitFor(() -> assertThat(eth.getTransactionReceipt(hash).isPresent()).isTrue());
    } catch (final ConditionTimeoutException e) {
      LOG.error("Timed out waiting for a block containing the transaction receipt hash: " + hash);
      throw new RuntimeException("No receipt found for hash: " + hash);
    }
  }
}

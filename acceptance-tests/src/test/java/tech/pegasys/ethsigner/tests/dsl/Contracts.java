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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.core.ConditionTimeoutException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class Contracts {

  private static final Logger LOG = LogManager.getLogger();

  private final Eth eth;
  private final Web3j jsonRpc;

  public Contracts(final Eth eth, final Web3j jsonRpc) {
    this.eth = eth;
    this.jsonRpc = jsonRpc;
  }

  public String submit(final Transaction smartContract) throws IOException {
    return eth.sendTransaction(smartContract);
  }

  public void awaitBlockContaining(final String hash) {
    try {
      waitFor(() -> assertThat(eth.getTransactionReceipt(hash).isPresent()).isTrue());
    } catch (final ConditionTimeoutException e) {
      LOG.error("Timed out waiting for a block containing the transaction receipt hash: " + hash);
    }
  }

  public String address(final String hash) throws IOException {
    final TransactionReceipt receipt =
        eth.getTransactionReceipt(hash)
            .orElseThrow(() -> new RuntimeException("No receipt found for hash: " + hash));
    assertThat(receipt.getContractAddress()).isNotEmpty();
    return receipt.getContractAddress();
  }

  public String code(final String address) throws IOException {
    final String code = eth.getCode(address);
    assertThat(code).isNotEmpty();
    return code;
  }

  public String call(final Transaction contractViewOperation) throws IOException {
    return jsonRpc
        .ethCall(contractViewOperation, DefaultBlockParameter.valueOf("latest"))
        .send()
        .getValue();
  }
}

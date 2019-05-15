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
import static tech.pegasys.ethsigner.tests.dsl.utils.ExceptionUtils.failOnIOException;

import java.math.BigInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.core.ConditionTimeoutException;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class Contracts {

  private static final Logger LOG = LogManager.getLogger();

  public static final BigInteger GAS_PRICE = BigInteger.valueOf(1000);
  public static final BigInteger GAS_LIMIT = BigInteger.valueOf(3000000);

  private final Eth eth;

  public Contracts(final Eth eth) {
    this.eth = eth;
  }

  public String submit(final Transaction smartContract) {
    return failOnIOException(() -> eth.sendTransaction(smartContract));
  }

  public void awaitBlockContaining(final String hash) {
    try {
      waitFor(() -> assertThat(eth.getTransactionReceipt(hash).isPresent()).isTrue());
    } catch (final ConditionTimeoutException e) {
      LOG.error("Timed out waiting for a block containing the transaction receipt hash: " + hash);
    }
  }

  public String address(final String hash) {
    return failOnIOException(
        () -> {
          final TransactionReceipt receipt =
              eth.getTransactionReceipt(hash)
                  .orElseThrow(() -> new RuntimeException("No receipt found for hash: " + hash));
          assertThat(receipt.getContractAddress()).isNotEmpty();
          return receipt.getContractAddress();
        });
  }

  public String code(final String address) {
    return failOnIOException(
        () -> {
          final String code = eth.getCode(address);
          assertThat(code).isNotEmpty();
          return code;
        });
  }

  public String call(final Transaction contractViewOperation) {
    return failOnIOException(() -> eth.call(contractViewOperation));
  }
}

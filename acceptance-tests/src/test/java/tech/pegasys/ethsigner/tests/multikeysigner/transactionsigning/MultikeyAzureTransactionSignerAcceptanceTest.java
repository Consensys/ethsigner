/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.ethsigner.tests.multikeysigner.transactionsigning;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import java.math.BigInteger;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class MultikeyAzureTransactionSignerAcceptanceTest extends
    MultikeyTransactionSigningAcceptanceTestBase {
  static final String clientId = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
  static final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");
  static final String FILENAME = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";

  @BeforeAll
  public void checkAzureCredentials() {
    Assumptions.assumeTrue(
        clientId != null && clientSecret != null,
        "Ensure Azure client id and client secret env variables are set");
  }

  @Test
  public void azureLoadedFromMultikeyCanSignValueTransferTransaction(@TempDir Path tomlDirectory) {
    createAzureTomlFileAt(
        "arbitrary_prefix" + FILENAME + ".toml", clientId, clientSecret, tomlDirectory);

    setUpBase(tomlDirectory);
    final BigInteger transferAmountWei = Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact();

    final BigInteger startBalance = ethNode.accounts().balance(RECIPIENT);
    final Transaction transaction =
        Transaction.createEtherTransaction(
            richBenefactor().address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            RECIPIENT,
            transferAmountWei);

    final String hash = ethSigner.transactions().submit(transaction);
    ethNode.transactions().awaitBlockContaining(hash);

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance = ethNode.accounts().balance(RECIPIENT);
    assertThat(actualEndBalance).isEqualTo(expectedEndBalance);
  }
}

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
package tech.pegasys.ethfirewall.tests;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class SignTransactionAcceptanceTest extends AcceptanceTestBase {

  private static final String LOCALHOST = "127.0.0.1";

  public static final String GENESIS_ACCOUNT_ONE_PRIVATE_KEY =
      "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";

  public static final String GENESIS_ACCOUNT_ONE_PUBLIC_KEY =
      "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73";

  public static final String GENESIS_ACCOUNT_ONE_PASSWORD = "pass";

  private final JsonRpc2_0Web3j jsonRpc =
      new JsonRpc2_0Web3j(
          new HttpService("http://" + LOCALHOST + ":" + 9945),
          2000,
          Async.defaultExecutorService());

  /** Number of GAS units that the transaction will cost. */
  private static final BigInteger INTRINSIC_GAS = BigInteger.valueOf(21000);

  @Test
  public void valueTransfer() {

    final BigInteger nonce = BigInteger.ZERO;
    final BigInteger gasPrice = BigInteger.valueOf(5);
    final BigDecimal transferAmount = new BigDecimal(15.5);
    final Unit transferUnit = Unit.ETHER;
    final String sender = address(GENESIS_ACCOUNT_ONE_PRIVATE_KEY);
    final String recipient = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";

    final Transaction transaction =
        Transaction.createEtherTransaction(
            sender,
            nonce,
            gasPrice,
            INTRINSIC_GAS,
            recipient,
            Convert.toWei(transferAmount, transferUnit).toBigIntegerExact());

    try {

      final String hash = jsonRpc.ethSendTransaction(transaction).send().getTransactionHash();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    // TODO value transfer

    // TODO verify deployment on node
  }

  @Test
  public void contract() {

    // TODO contract

    // TODO verify deployment on node
  }

  public static String address(String privateKeyInHex) {
    BigInteger privateKeyInBT = new BigInteger(privateKeyInHex, 16);
    ECKeyPair aPair = ECKeyPair.create(privateKeyInBT);
    BigInteger publicKeyInBT = aPair.getPublicKey();
    String sPublickeyInHex = publicKeyInBT.toString(16);
    return sPublickeyInHex;
  }
}

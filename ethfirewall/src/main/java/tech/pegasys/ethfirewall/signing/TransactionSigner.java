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
package tech.pegasys.ethfirewall.signing;

import tech.pegasys.ethfirewall.signing.web3j.TransactionEncoder;

import java.math.BigInteger;

import io.vertx.core.json.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.utils.Numeric;

public class TransactionSigner {

  private final Credentials credentials;
  private final ChainIdProvider chain;

  public TransactionSigner(final ChainIdProvider chain, final Credentials credentials) {
    this.chain = chain;
    this.credentials = credentials;
  }

  public String signTransaction(final JsonObject transaction) {
    final RawTransaction rawTransaction = fromTransactionJson(transaction);

    // Sign the transaction using the post Spurious Dragon technique
    final byte[] signedMessage =
        TransactionEncoder.signMessage(rawTransaction, chain.id(), credentials);
    return Numeric.toHexString(signedMessage);
  }

  private RawTransaction fromTransactionJson(final JsonObject transaction) {
    final JsonObject txnParams = transaction.getJsonArray("params").getJsonObject(0);
    String dataString = "";
    if (txnParams.getString("data") != null) {
      dataString = txnParams.getString("data");
    }

    // TODO(tmm): This should be configurable, but currently matches Geth.
    String gasValue = "90000";
    if (txnParams.getString("gas") != null) {
      gasValue = txnParams.getString("gas").substring(2);
    }

    return RawTransaction.createTransaction(
        new BigInteger(txnParams.getString("nonce").substring(2), 16),
        new BigInteger(txnParams.getString("gasPrice").substring(2), 16),
        new BigInteger(gasValue, 16),
        txnParams.getString("to"),
        new BigInteger(txnParams.getString("value").substring(2), 16),
        dataString);
  }
}

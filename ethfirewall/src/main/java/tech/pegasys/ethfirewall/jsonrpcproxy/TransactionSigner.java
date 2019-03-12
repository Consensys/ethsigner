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
package tech.pegasys.ethfirewall.jsonrpcproxy;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import io.vertx.core.json.JsonObject;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

public class TransactionSigner {

  private Credentials credentials;

  public TransactionSigner(final Credentials credentials) {
    this.credentials = credentials;
  }

  public static TransactionSigner createFrom(final File keyFile, final String password)
      throws IOException, CipherException {
    final Credentials credentials = WalletUtils.loadCredentials(password, keyFile);

    return new TransactionSigner(credentials);
  }

  public String signTransaction(final JsonObject transaction) {
    final RawTransaction rawTransaction = fromTransactionJson(transaction);

    final byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
    return Numeric.toHexString(signedMessage);
  }

  public static RawTransaction fromTransactionJson(final JsonObject transaction) {
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

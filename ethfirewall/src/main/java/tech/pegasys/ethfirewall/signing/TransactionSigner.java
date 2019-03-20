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

import java.math.BigInteger;

import io.vertx.core.json.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class TransactionSigner {

  private static final int HEXADECIMAL = 16;
  private static final int HEXADECIMAL_PREFIX_LENGTH = 2;

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
    final JsonObject params = transaction.getJsonArray("params").getJsonObject(0);

    // TODO validate the nonce is present, not null
    return RawTransaction.createTransaction(
        parseNonce(transaction, params),
        hex("gasPrice", params),
        gas(params),
        params.getString("to"),
        hex("value", params),
        data(params));
  }

  private BigInteger parseNonce(final JsonObject transaction, final JsonObject params) {

    if (params.containsKey("nonce")) {
      return hex("nonce", params);
    }

    throw new IllegalArgumentException("Missing the nonce%n" + transaction.encodePrettily());
  }

  private String data(final JsonObject params) {
    return params.getString("data");
  }

  private BigInteger gas(final JsonObject params) {
    // TODO(tmm): This should be configurable, but currently matches Geth.
    String gasValue = "90000";

    if (params.getString("gas") != null) {
      gasValue = params.getString("gas").substring(HEXADECIMAL_PREFIX_LENGTH);
    }

    return hex(gasValue);
  }

  // TODO validate hex

  private BigInteger hex(final String value) {
    return new BigInteger(value, HEXADECIMAL);
  }

  private BigInteger hex(final String key, final JsonObject params) {
    return params.containsKey(key)
        ? new BigInteger(params.getString(key).substring(HEXADECIMAL_PREFIX_LENGTH), HEXADECIMAL)
        : null;
  }
}

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

import io.vertx.core.json.JsonObject;
import java.math.BigInteger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class TransactionSigner {

  private static final int HEXADECIMAL = 16;
  private static final int HEXADECIMAL_PREFIX_LENGTH = 2;

  private final Credentials credentials;
  private final ChainIdProvider chain;
  private final MandatoryTransactionNonce nonce;

  public TransactionSigner(final ChainIdProvider chain, final Credentials credentials) {
    this.chain = chain;
    this.credentials = credentials;
    this.nonce = new MandatoryTransactionNonce();
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

    return RawTransaction.createTransaction(
        nonce.get(transaction),
        optionalHex("gasPrice", params),
        gas(params),
        params.getString("to"),
        optionalHex("value", params),
        data(params));
  }

  private String data(final JsonObject params) {
    return params.getString("data");
  }

  private BigInteger gas(final JsonObject params) {

    if (params.getString("gas") != null) {
      return hex(params.getString("gas").substring(HEXADECIMAL_PREFIX_LENGTH));
    }

    // TODO(tmm): This should be configurable, but currently matches Geth.
    return new BigInteger("90000");
  }

  private BigInteger hex(final String value) {
    return new BigInteger(value, HEXADECIMAL);
  }

  // TODO validate hex format - prefix 0x
  private BigInteger optionalHex(final String key, final JsonObject params) {
    return params.containsKey(key)
        ? new BigInteger(params.getString(key).substring(HEXADECIMAL_PREFIX_LENGTH), HEXADECIMAL)
        : null;
  }
}

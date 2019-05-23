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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import tech.pegasys.ethsigner.core.jsonrpc.EthSendTransactionJsonParameters;

import java.math.BigInteger;

import com.google.common.base.MoreObjects;
import org.web3j.crypto.RawTransaction;

public class RawTransactionBuilder {

  private BigInteger nonce;
  private BigInteger gasPrice;
  private BigInteger gasLimit;
  private String to;
  private BigInteger value;
  private String data;

  public RawTransactionBuilder(
      final BigInteger nonce,
      final BigInteger gasPrice,
      final BigInteger gasLimit,
      final String to,
      final BigInteger value,
      final String data) {
    this.nonce = nonce;
    this.gasPrice = gasPrice;
    this.gasLimit = gasLimit;
    this.to = to;
    this.value = value;
    this.data = data;
  }

  public static RawTransactionBuilder from(final EthSendTransactionJsonParameters input) {
    return new RawTransactionBuilder(
        input.nonce().orElse(null),
        input.gasPrice().orElse(BigInteger.ZERO),
        input.gas().orElse(BigInteger.valueOf(90000)),
        input.receiver().orElse(""),
        input.value().orElse(BigInteger.ZERO),
        input.data().orElse(""));
  }

  public RawTransactionBuilder withNonce(final BigInteger nonce) {
    this.nonce = nonce;
    return this;
  }

  public RawTransaction build() {
    return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nonce", nonce)
        .add("gasPrice", gasPrice)
        .add("gasLimit", gasLimit)
        .add("to", to)
        .add("value", value)
        .add("data", data)
        .toString();
  }
}

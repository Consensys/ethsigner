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
package tech.pegasys.ethsigner.requesthandler.sendtransaction;

import tech.pegasys.ethsigner.jsonrpc.SendTransactionJsonParameters;

import java.math.BigInteger;

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

  public static RawTransactionBuilder from(final SendTransactionJsonParameters input) {
    return new RawTransactionBuilder(
        input.nonce().orElse(null),
        input.gasPrice().orElse(BigInteger.ZERO),
        input.gas().orElse(BigInteger.valueOf(90000)),
        input.receiver().orElse(""),
        input.value().orElse(BigInteger.ZERO),
        input.data());
  }

  public RawTransactionBuilder updateNonce(final BigInteger nonce) {
    this.nonce = nonce;
    return this;
  }

  public RawTransaction build() {
    return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
  }

  @Override
  public String toString() {
    return "RawTransactionBuilder{"
        + "nonce="
        + nonce
        + ", gasPrice="
        + gasPrice
        + ", gasLimit="
        + gasLimit
        + ", to='"
        + to
        + '\''
        + ", value="
        + value
        + ", data='"
        + data
        + '\''
        + '}';
  }
}

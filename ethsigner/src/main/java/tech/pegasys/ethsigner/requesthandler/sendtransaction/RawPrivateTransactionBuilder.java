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

import tech.pegasys.ethsigner.jsonrpc.EeaSendTransactionJsonParameters;

import java.math.BigInteger;
import java.util.List;

import com.google.common.base.MoreObjects;
import org.web3j.protocol.eea.crypto.RawPrivateTransaction;

public class RawPrivateTransactionBuilder {

  private BigInteger nonce;
  private BigInteger gasPrice;
  private BigInteger gasLimit;
  private String to;
  private BigInteger value;
  private String data;
  private String privateFrom;
  private List<String> privateFor;
  private String restriction;

  public RawPrivateTransactionBuilder(
      final BigInteger nonce,
      final BigInteger gasPrice,
      final BigInteger gasLimit,
      final String to,
      final BigInteger value,
      final String data,
      final String privateFrom,
      final List<String> privateFor,
      final String restriction) {
    this.nonce = nonce;
    this.gasPrice = gasPrice;
    this.gasLimit = gasLimit;
    this.to = to;
    this.value = value;
    this.data = data;
    this.privateFrom = privateFrom;
    this.privateFor = privateFor;
    this.restriction = restriction;
  }

  public static RawPrivateTransactionBuilder from(final EeaSendTransactionJsonParameters input) {
    return new RawPrivateTransactionBuilder(
        input.nonce().orElse(null),
        input.gasPrice().orElse(BigInteger.ZERO),
        input.gas().orElse(BigInteger.valueOf(90000)),
        input.receiver().orElse(""),
        input.value().orElse(BigInteger.ZERO),
        input.data().orElse(""),
        input.privateFrom(),
        input.privateFor(),
        input.restriction());
  }

  public RawPrivateTransactionBuilder updateNonce(final BigInteger nonce) {
    this.nonce = nonce;
    return this;
  }

  public RawPrivateTransaction build() {
    return RawPrivateTransaction.createTransaction(
        nonce, gasPrice, gasLimit, to, data, privateFrom, privateFor, restriction);
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
        .add("privateFrom", privateFrom)
        .add("privateFor", privateFor)
        .add("restriction", restriction)
        .toString();
  }
}

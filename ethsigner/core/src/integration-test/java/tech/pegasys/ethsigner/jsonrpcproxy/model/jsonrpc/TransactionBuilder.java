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
package tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc;

public class TransactionBuilder {
  private String from;
  private String nonce;
  private String gasPrice;
  private String gas;
  private String to;
  private String value;
  private String data;

  public TransactionBuilder withFrom(final String from) {
    this.from = from;
    return this;
  }

  public TransactionBuilder removeFrom() {
    this.from = null;
    return this;
  }

  public TransactionBuilder withNonce(final String nonce) {
    this.nonce = nonce;
    return this;
  }

  public TransactionBuilder removeNonce() {
    this.nonce = null;
    return this;
  }

  public TransactionBuilder withGasPrice(final String gasPrice) {
    this.gasPrice = gasPrice;
    return this;
  }

  public TransactionBuilder removeGasPrice() {
    this.gasPrice = null;
    return this;
  }

  public TransactionBuilder withGas(final String gas) {
    this.gas = gas;
    return this;
  }

  public TransactionBuilder removeGas() {
    this.gas = null;
    return this;
  }

  public TransactionBuilder withTo(final String to) {
    this.to = to;
    return this;
  }

  public TransactionBuilder removeTo() {
    this.to = null;
    return this;
  }

  public TransactionBuilder withValue(final String value) {
    this.value = value;
    return this;
  }

  public TransactionBuilder removeValue() {
    this.value = null;
    return this;
  }

  public TransactionBuilder withData(final String data) {
    this.data = data;
    return this;
  }

  public TransactionBuilder removeData() {
    this.data = null;
    return this;
  }

  public Transaction build() {
    return new Transaction(from, nonce, gasPrice, gas, to, value, data);
  }
}

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

import java.util.List;

public class PrivateTransactionBuilder {
  private String from;
  private String nonce;
  private String gasPrice;
  private String gas;
  private String to;
  private String value;
  private String data;
  private String privateFrom;
  private List<String> privateFor;
  private String restriction;

  public PrivateTransactionBuilder withFrom(final String from) {
    this.from = from;
    return this;
  }

  public PrivateTransactionBuilder removeFrom() {
    this.from = null;
    return this;
  }

  public PrivateTransactionBuilder withNonce(final String nonce) {
    this.nonce = nonce;
    return this;
  }

  public PrivateTransactionBuilder removeNonce() {
    this.nonce = null;
    return this;
  }

  public PrivateTransactionBuilder withGasPrice(final String gasPrice) {
    this.gasPrice = gasPrice;
    return this;
  }

  public PrivateTransactionBuilder removeGasPrice() {
    this.gasPrice = null;
    return this;
  }

  public PrivateTransactionBuilder withGas(final String gas) {
    this.gas = gas;
    return this;
  }

  public PrivateTransactionBuilder removeGas() {
    this.gas = null;
    return this;
  }

  public PrivateTransactionBuilder withTo(final String to) {
    this.to = to;
    return this;
  }

  public PrivateTransactionBuilder removeTo() {
    this.to = null;
    return this;
  }

  public PrivateTransactionBuilder withValue(final String value) {
    this.value = value;
    return this;
  }

  public PrivateTransactionBuilder removeValue() {
    this.value = null;
    return this;
  }

  public PrivateTransactionBuilder withData(final String data) {
    this.data = data;
    return this;
  }

  public PrivateTransactionBuilder removeData() {
    this.data = null;
    return this;
  }

  public PrivateTransactionBuilder withPrivateFrom(final String privateFrom) {
    this.privateFrom = privateFrom;
    return this;
  }

  public PrivateTransactionBuilder removePrivateFrom() {
    this.privateFrom = null;
    return this;
  }

  public PrivateTransactionBuilder withPrivateFor(final List<String> privateFor) {
    this.privateFor = privateFor;
    return this;
  }

  public PrivateTransactionBuilder removePrivateFor() {
    this.privateFor = null;
    return this;
  }

  public PrivateTransactionBuilder withRestriction(final String restriction) {
    this.restriction = restriction;
    return this;
  }

  public PrivateTransactionBuilder removeRestriction() {
    this.restriction = null;
    return this;
  }

  public PrivateTransaction build() {
    return new PrivateTransaction(
        from, nonce, gasPrice, gas, to, value, data, privateFrom, privateFor, restriction);
  }
}

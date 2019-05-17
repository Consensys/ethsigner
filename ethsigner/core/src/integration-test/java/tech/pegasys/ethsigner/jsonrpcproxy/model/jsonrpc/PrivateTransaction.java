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

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateTransaction {
  private final String from;
  private final String to;
  private final String gasPrice;
  private final String gas;
  private final String value;
  private final String data;
  private final String nonce;
  private final String privateFrom;
  private final List<String> privateFor;
  private final String restriction;

  public PrivateTransaction(
      final String from,
      final String nonce,
      final String gasPrice,
      final String gas,
      final String to,
      final String value,
      final String data,
      final String privateFrom,
      final List<String> privateFor,
      final String restriction) {
    this.from = from;
    this.nonce = nonce;
    this.gasPrice = gasPrice;
    this.gas = gas;
    this.to = to;
    this.value = value;
    this.data = data;
    this.privateFrom = privateFrom;
    this.privateFor = privateFor;
    this.restriction = restriction;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public String getGasPrice() {
    return gasPrice;
  }

  public String getGas() {
    return gas;
  }

  public String getValue() {
    return value;
  }

  public String getData() {
    return data;
  }

  public String getNonce() {
    return nonce;
  }

  public String getPrivateFrom() {
    return privateFrom;
  }

  public List<String> getPrivateFor() {
    return privateFor;
  }

  public String getRestriction() {
    return restriction;
  }

  public static class PrivateTransactionBuilder {
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

    public PrivateTransactionBuilder withNonce(final String nonce) {
      this.nonce = nonce;
      return this;
    }

    public PrivateTransactionBuilder withGasPrice(final String gasPrice) {
      this.gasPrice = gasPrice;
      return this;
    }

    public PrivateTransactionBuilder withGas(final String gas) {
      this.gas = gas;
      return this;
    }

    public PrivateTransactionBuilder withTo(final String to) {
      this.to = to;
      return this;
    }

    public PrivateTransactionBuilder withValue(final String value) {
      this.value = value;
      return this;
    }

    public PrivateTransactionBuilder withData(final String data) {
      this.data = data;
      return this;
    }

    public PrivateTransactionBuilder withPrivateFrom(final String privateFrom) {
      this.privateFrom = privateFrom;
      return this;
    }

    public PrivateTransactionBuilder withPrivateFor(final List<String> privateFor) {
      this.privateFor = privateFor;
      return this;
    }

    public PrivateTransactionBuilder withRestriction(final String restriction) {
      this.restriction = restriction;
      return this;
    }

    public PrivateTransaction build() {
      return new PrivateTransaction(
          from, nonce, gasPrice, gas, to, value, data, privateFrom, privateFor, restriction);
    }
  }
}

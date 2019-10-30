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

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {
  private final String from;
  private final String to;
  private final String gasPrice;
  private final String gas;
  private final String value;
  private final String data;
  private final String nonce;

  public Transaction(
      final String from,
      final String nonce,
      final String gasPrice,
      final String gas,
      final String to,
      final String value,
      final String data) {
    this.from = from;
    this.nonce = nonce;
    this.gasPrice = gasPrice;
    this.gas = gas;
    this.to = to;
    this.value = value;
    this.data = data;
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
}

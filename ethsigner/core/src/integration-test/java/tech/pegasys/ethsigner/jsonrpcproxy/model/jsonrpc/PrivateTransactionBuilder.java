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

import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendTransaction.FIELD_PRIVATE_FOR;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendTransaction.FIELD_PRIVATE_FROM;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.EeaSendTransaction.FIELD_RESTRICTION;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_DATA;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_FROM;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS_PRICE;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_NONCE;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_TO;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_VALUE;

import java.util.List;

import io.vertx.core.json.JsonObject;

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

  public JsonObject build() {
    final JsonObject jsonObject = new JsonObject();
    jsonObject.put(FIELD_FROM, from);
    jsonObject.put(FIELD_NONCE, nonce);
    jsonObject.put(FIELD_GAS_PRICE, gasPrice);
    jsonObject.put(FIELD_GAS, gas);
    jsonObject.put(FIELD_TO, to);
    jsonObject.put(FIELD_VALUE, value);
    jsonObject.put(FIELD_DATA, data);
    jsonObject.put(FIELD_PRIVATE_FROM, privateFrom);
    jsonObject.put(FIELD_PRIVATE_FOR, privateFor);
    jsonObject.put(FIELD_RESTRICTION, restriction);
    return jsonObject;
  }
}

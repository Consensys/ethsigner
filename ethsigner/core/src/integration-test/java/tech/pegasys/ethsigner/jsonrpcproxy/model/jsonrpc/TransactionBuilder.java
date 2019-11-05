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

import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_DATA;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_FROM;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS_PRICE;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_NONCE;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_TO;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_VALUE;

import io.vertx.core.json.JsonObject;

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

  public TransactionBuilder withNonce(final String nonce) {
    this.nonce = nonce;
    return this;
  }

  public TransactionBuilder withGasPrice(final String gasPrice) {
    this.gasPrice = gasPrice;
    return this;
  }

  public TransactionBuilder withGas(final String gas) {
    this.gas = gas;
    return this;
  }

  public TransactionBuilder withTo(final String to) {
    this.to = to;
    return this;
  }

  public TransactionBuilder withValue(final String value) {
    this.value = value;
    return this;
  }

  public TransactionBuilder withData(final String data) {
    this.data = data;
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
    return jsonObject;
  }
}

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
package tech.pegasys.ethfirewall.jsonrpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendTransactionJsonParameters {

  private final String data;
  private String gas;
  private String gasPrice;
  private String nonce;
  private String receiver;
  private String value;

  @JsonCreator
  public SendTransactionJsonParameters(@JsonProperty("data") final String data) {
    this.data = data;
  }

  @JsonSetter("gas")
  public void gas(final String gas) {
    this.gas = gas;
  }

  @JsonSetter("gasPrice")
  public void gasPrice(final String gasPrice) {
    this.gasPrice = gasPrice;
  }

  @JsonSetter("nonce")
  public void nonce(final String nonce) {
    this.nonce = nonce;
  }

  @JsonSetter("to")
  public void receiver(final String receiver) {
    this.receiver = receiver;
  }

  @JsonSetter("value")
  public void value(final String value) {
    this.value = value;
  }

  public String data() {
    return data;
  }

  public Optional<String> gas() {
    return Optional.ofNullable(gas);
  }

  public Optional<String> gasPrice() {
    return Optional.ofNullable(gasPrice);
  }

  public Optional<String> receiver() {
    return Optional.ofNullable(receiver);
  }

  public Optional<String> value() {
    return Optional.ofNullable(value);
  }

  public Optional<String> nonce() {
    return Optional.ofNullable(nonce);
  }
}

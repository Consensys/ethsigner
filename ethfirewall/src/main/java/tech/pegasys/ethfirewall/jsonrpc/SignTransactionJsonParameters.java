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

@JsonIgnoreProperties(ignoreUnknown = true)
public class SignTransactionJsonParameters {

  private final String data;
  private final String gas;
  private final String gasPrice;
  private final String nonce;
  private final String receiver;
  private final String value;

  @JsonCreator
  public SignTransactionJsonParameters(
      @JsonProperty("data") final String data,
      @JsonProperty("gas") final String gas,
      @JsonProperty("gasPrice") final String gasPrice,
      @JsonProperty("nonce") final String nonce,
      @JsonProperty("to") final String receiverAddress,
      @JsonProperty("value") final String value) {
    this.data = data;
    this.gas = gas;
    this.gasPrice = gasPrice;
    this.nonce = nonce;
    this.receiver = receiverAddress;
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

  public String receiver() {
    return receiver;
  }

  public Optional<String> value() {
    return Optional.ofNullable(value);
  }

  public Optional<String> nonce() {
    return Optional.ofNullable(nonce);
  }
}

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

import java.math.BigInteger;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendTransactionJsonParameters {

  private static final int HEXADECIMAL = 16;
  private static final int HEXADECIMAL_PREFIX_LENGTH = 2;

  private final String data;
  private String sender;

  private BigInteger gas;
  private BigInteger gasPrice;
  private BigInteger nonce;
  private BigInteger value;
  private String receiver;

  @JsonCreator
  public SendTransactionJsonParameters(
      @JsonProperty("from") final String sender, @JsonProperty("data") final String data) {
    this.data = data;
    this.sender = sender;
  }

  @JsonSetter("gas")
  public void gas(final String gas) {
    this.gas = optionalHex(gas);
  }

  @JsonSetter("gasPrice")
  public void gasPrice(final String gasPrice) {
    this.gasPrice = optionalHex(gasPrice);
  }

  @JsonSetter("nonce")
  public void nonce(final String nonce) {
    this.nonce = optionalHex(nonce);
  }

  @JsonSetter("to")
  public void receiver(final String receiver) {
    this.receiver = receiver;
  }

  @JsonSetter("value")
  public void value(final String value) {
    this.value = optionalHex(value);
  }

  public String data() {
    return data;
  }

  public Optional<BigInteger> gas() {
    return Optional.ofNullable(gas);
  }

  public Optional<BigInteger> gasPrice() {
    return Optional.ofNullable(gasPrice);
  }

  public Optional<String> receiver() {
    return Optional.ofNullable(receiver);
  }

  public Optional<BigInteger> value() {
    return Optional.ofNullable(value);
  }

  public Optional<BigInteger> nonce() {
    return Optional.ofNullable(nonce);
  }

  public String sender() {
    return sender;
  }

  private BigInteger hex(final String value) {
    return new BigInteger(value, HEXADECIMAL);
  }

  // TODO validate hex format - ie. has prefix 0x
  // TODO exception on parse?
  private BigInteger optionalHex(final String value) {
    return hex(value.substring(HEXADECIMAL_PREFIX_LENGTH));
  }
}

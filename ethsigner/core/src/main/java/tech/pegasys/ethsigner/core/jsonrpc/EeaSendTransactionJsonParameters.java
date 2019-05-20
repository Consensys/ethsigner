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
package tech.pegasys.ethsigner.core.jsonrpc;

import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.fromRpcRequest;
import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.optionalHex;
import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.validatePrefix;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EeaSendTransactionJsonParameters {
  private final String sender;
  private final String privateFrom;
  private final List<String> privateFor;
  private final String restriction;

  private BigInteger gas;
  private BigInteger gasPrice;
  private BigInteger nonce;
  private BigInteger value;
  private String receiver;
  private String data;

  @JsonCreator
  public EeaSendTransactionJsonParameters(
      @JsonProperty("from") final String sender,
      @JsonProperty("privateFrom") final String privateFrom,
      @JsonProperty("privateFor") final List<String> privateFor,
      @JsonProperty("restriction") final String restriction) {
    validatePrefix(sender);
    this.privateFrom = privateFrom;
    this.privateFor = privateFor;
    this.restriction = restriction;
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
    validatePrefix(receiver);
    this.receiver = receiver;
  }

  @JsonSetter("value")
  public void value(final String value) {
    this.value = optionalHex(value);
  }

  @JsonSetter("data")
  public void data(final String data) {
    this.data = data;
  }

  public Optional<String> data() {
    return Optional.ofNullable(data);
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

  public String privateFrom() {
    return privateFrom;
  }

  public List<String> privateFor() {
    return privateFor;
  }

  public String restriction() {
    return restriction;
  }

  public static EeaSendTransactionJsonParameters from(final JsonRpcRequest request) {
    return fromRpcRequest(EeaSendTransactionJsonParameters.class, request);
  }
}

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

import static org.web3j.utils.Numeric.decodeQuantity;
import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.decodeBigInteger;
import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.fromRpcRequestToJsonParam;
import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.validateNotEmpty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.web3j.utils.Base64String;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EthSendTransactionJsonParameters {

  private final String sender;
  private BigInteger gas;
  private BigInteger gasPrice;
  private BigInteger nonce;
  private BigInteger value;
  private String receiver;
  private String data;

  private Base64String privateFrom = null;
  private List<Base64String> privateFor;
  private String restriction = null;
  private Base64String privacyGroupId;

  @JsonCreator
  public EthSendTransactionJsonParameters(@JsonProperty("from") final String sender) {
    validateNotEmpty(sender);
    this.sender = sender;
  }

  @JsonSetter("gas")
  public void gas(final String gas) {
    this.gas = decodeBigInteger(gas);
  }

  @JsonSetter("gasPrice")
  public void gasPrice(final String gasPrice) {
    this.gasPrice = decodeBigInteger(gasPrice);
  }

  @JsonSetter("nonce")
  public void nonce(final String nonce) {
    this.nonce = decodeBigInteger(nonce);
  }

  @JsonSetter("to")
  public void receiver(final String receiver) {
    this.receiver = receiver;
  }

  @JsonSetter("value")
  public void value(final String value) {
    validateValue(value);
    this.value = decodeBigInteger(value);
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

  @JsonProperty("from")
  public String sender() {
    return sender;
  }

  @JsonProperty("privateFrom")
  public void privateFrom(String privateFrom) {
    if (privateFrom != null) {
      this.privateFrom = Base64String.wrap(privateFrom);
    }
  }

  public Optional<Base64String> privateFrom() {
    return Optional.ofNullable(privateFrom);
  }

  @JsonProperty("privateFor")
  public void privateFor(final String[] privateFor) {
    this.privateFor =
        Arrays.stream(privateFor).map(Base64String::wrap).collect(Collectors.toList());
  }

  public Optional<List<Base64String>> privateFor() {
    return Optional.ofNullable(privateFor);
  }

  @JsonProperty("privacyGroupId")
  public void privacyGroupId(final String privacyGroupId) {
    this.privacyGroupId = Base64String.wrap(privacyGroupId);
  }

  public Optional<Base64String> privacyGroupId() {
    return Optional.ofNullable(privacyGroupId);
  }

  public void restriction(String restriction) {
    this.restriction = restriction;
  }

  public Optional<String> restriction() {
    return Optional.ofNullable(restriction);
  }

  public static EthSendTransactionJsonParameters from(final JsonRpcRequest request) {
    return fromRpcRequestToJsonParam(EthSendTransactionJsonParameters.class, request);
  }

  public boolean isPrivate() {
    return privacyGroupId().isPresent()
        || privateFor().isPresent()
        || privateFrom().isPresent()
        || restriction().isPresent();
  }

  private void validateValue(final String value) {
    if (isPrivate() && value != null && !decodeQuantity(value).equals(BigInteger.ZERO)) {
      throw new IllegalArgumentException(
          "Non-zero value, private transactions cannot transfer ether");
    }
  }
}

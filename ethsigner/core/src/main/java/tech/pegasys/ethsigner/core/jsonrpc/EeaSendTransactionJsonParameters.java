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
import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.fromRpcRequestToJsonParam;
import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.validatePrefix;

import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.PrivacyIdentifier;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Lists;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EeaSendTransactionJsonParameters {

  private final String sender;
  private final PrivacyIdentifier privateFrom;
  private final String restriction;

  private BigInteger gas;
  private BigInteger gasPrice;
  private BigInteger nonce;
  private BigInteger value;
  private String receiver;
  private String data;
  private PrivacyIdentifier privacyGroupId;
  private List<PrivacyIdentifier> privateFor;

  @JsonCreator
  public EeaSendTransactionJsonParameters(
      @JsonProperty("from") final String sender,
      @JsonProperty("privateFrom") final String privateFrom,
      @JsonProperty("privateFor") final List<String> privateFor,
      @JsonProperty("restriction") final String restriction) {
    validatePrefix(sender);
    this.privateFrom = PrivacyIdentifier.fromBase64String(privateFrom);
    this.privateFor =
        privateFor.stream().map(PrivacyIdentifier::fromBase64String).collect(Collectors.toList());
    this.restriction = restriction;
    this.sender = sender;
  }

  @JsonSetter("gas")
  public void gas(final String gas) {
    this.gas = decodeQuantity(gas);
  }

  @JsonSetter("gasPrice")
  public void gasPrice(final String gasPrice) {
    this.gasPrice = decodeQuantity(gasPrice);
  }

  @JsonSetter("nonce")
  public void nonce(final String nonce) {
    this.nonce = decodeQuantity(nonce);
  }

  @JsonSetter("to")
  public void receiver(final String receiver) {
    validatePrefix(receiver);
    this.receiver = receiver;
  }

  @JsonSetter("value")
  public void value(final String value) {
    validateValue(value);
    this.value = decodeQuantity(value);
  }

  @JsonSetter("data")
  public void data(final String data) {
    this.data = data;
  }

  @JsonSetter("privateFor")
  public void privateFor(final String[] privateFor) {
    this.privateFor =
        Lists.newArrayList(privateFor).stream()
            .map(PrivacyIdentifier::fromBase64String)
            .collect(Collectors.toList());
  }

  @JsonSetter("privacyGroupId")
  public void privacyGroupId(final String privacyGroupId) {
    this.privacyGroupId = PrivacyIdentifier.fromBase64String(privacyGroupId);
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

  public Optional<List<PrivacyIdentifier>> privateFor() {
    return Optional.ofNullable(privateFor);
  }

  public Optional<PrivacyIdentifier> privacyGroupId() {
    return Optional.ofNullable(privacyGroupId);
  }

  public String sender() {
    return sender;
  }

  public PrivacyIdentifier privateFrom() {
    return privateFrom;
  }

  public String restriction() {
    return restriction;
  }

  public static EeaSendTransactionJsonParameters from(final JsonRpcRequest request) {
    return fromRpcRequestToJsonParam(EeaSendTransactionJsonParameters.class, request);
  }

  private void validateValue(final String value) {
    if (!decodeQuantity(value).equals(BigInteger.ZERO)) {
      throw new IllegalArgumentException(
          "Non-zero value, private transactions cannot transfer ether");
    }
  }
}

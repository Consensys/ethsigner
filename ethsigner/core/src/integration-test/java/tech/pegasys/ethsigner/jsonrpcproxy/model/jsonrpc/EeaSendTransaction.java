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

import static java.util.Collections.singletonList;

import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.PrivateTransaction.PrivateTransactionBuilder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.Json;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class EeaSendTransaction {
  private static final String RESTRICTED = "restricted";
  private static final int DEFAULT_ID = 77;
  private static final String UNLOCKED_ACCOUNT = "0x7577919ae5df4941180eac211965f275cdce314d";
  private static final String PRIVATE_FROM = "ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI=";
  private static final List<String> PRIVATE_FOR =
      singletonList("GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w=");

  public String withGas(final String gas) {
    return replaceParameter("gas", gas, request());
  }

  public String withGasPrice(final String gasPrice) {
    return replaceParameter("gasPrice", gasPrice, request());
  }

  public String withValue(final String value) {
    return replaceParameter("value", value, request());
  }

  public String withNonce(final String nonce) {
    return replaceParameter("nonce", nonce, request());
  }

  public String withSender(final String receiver) {
    return replaceParameter("from", receiver, request());
  }

  public String withReceiver(final String sender) {
    return replaceParameter("to", sender, request());
  }

  public String missingSender() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingNonce() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingReceiver() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(UNLOCKED_ACCOUNT)
            .withPrivateFor(singletonList("0xd46e8dd67c5d32be8058bb8eb970870f07244567"))
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingValue() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingGas() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingGasPrice() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(UNLOCKED_ACCOUNT)
            .withPrivateFor(singletonList("0xd46e8dd67c5d32be8058bb8eb970870f07244567"))
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingData() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingPrivateFrom() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingPrivateFor() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(UNLOCKED_ACCOUNT)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String missingRestriction() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  /**
   * Due to the underlying server mocking, When only a single request is used, the contents does not
   * actually matter, only their equivalence does.
   */
  public String request() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
            .withGasPrice("0x9184e72a000")
            .withGas("0x76c0")
            .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
            .withValue("0x0")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  public String smartContract() {
    final PrivateTransaction transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withNonce("0x1")
            .withData(
                "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED)
            .build();
    return Json.encode(eeaSendTransaction(transaction));
  }

  private String replaceParameter(
      final String key, final String replacementValue, final String body) {
    final Pattern nonceWithValue = Pattern.compile(String.format("%s\\\":\\\"(\\w*)\\\"", key));
    final Matcher matches = nonceWithValue.matcher(body);
    return matches.replaceFirst(String.format("%s\":\"%s\"", key, replacementValue));
  }

  private Request<Object, EthSendTransaction> eeaSendTransaction(final Object transaction) {
    final Request<Object, EthSendTransaction> eea_sendTransaction =
        new Request<>(
            "eea_sendTransaction", singletonList(transaction), null, EthSendTransaction.class);
    eea_sendTransaction.setId(DEFAULT_ID);
    return eea_sendTransaction;
  }
}

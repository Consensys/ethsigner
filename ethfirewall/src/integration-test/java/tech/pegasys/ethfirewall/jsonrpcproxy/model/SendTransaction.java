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
package tech.pegasys.ethfirewall.jsonrpcproxy.model;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.Json;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;

public class SendTransaction {

  private static final int DEFAULT_ID = 77;

  private final Web3j jsonRpc;

  public SendTransaction(final Web3j jsonRpc) {
    this.jsonRpc = jsonRpc;
  }

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

  public String withReceiver(final String receiver) {
    return replaceParameter("from", receiver, request());
  }

  public String withSender(final String sender) {
    return replaceParameter("to", sender, request());
  }

  public String missingSender() {
    final Transaction transaction =
        new Transaction(
            null,
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  public String missingNonce() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            null,
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  public String missingReceiver() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            null,
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  public String missingValue() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            null,
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  public String missingGas() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            null,
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  public String missingGasPrice() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            null,
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  public String missingData() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            null);

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  /**
   * Due to the underlying server mocking, When only a single request is used, the contents does not
   * actually matter, only their equivalence does.
   */
  public String request() {
    final Transaction transaction =
        new Transaction(
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
            new BigInteger(
                "101454411220705080123888225389655371100299455501706857686025051036223022797554"),
            new BigInteger("10000000000000"),
            new BigInteger("30400"),
            "0xd46e8dd67c5d32be8058bb8eb970870f07244567",
            new BigInteger("2441406250"),
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  public String smartContract() {
    final Transaction transaction =
        new Transaction(
            "0xae8ed09c458cebc142c06bdd297709575482b0fd",
            new BigInteger("1"),
            null,
            null,
            null,
            null,
            "0x608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a");

    final Request<?, ? extends Response<?>> sendTransactionRequest =
        jsonRpc.ethSendTransaction(transaction);
    sendTransactionRequest.setId(DEFAULT_ID);
    return Json.encode(sendTransactionRequest);
  }

  private String replaceParameter(
      final String key, final String replacementValue, final String body) {
    final Pattern nonceWithValue = Pattern.compile(String.format("%s\\\":\\\"(\\w*)\\\"", key));
    final Matcher matches = nonceWithValue.matcher(body);
    return matches.replaceFirst(String.format("%s\":\"%s\"", key, replacementValue));
  }
}

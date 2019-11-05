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
import static tech.pegasys.ethsigner.jsonrpcproxy.IntegrationTestBase.DEFAULT_ID;

import java.util.List;

import io.vertx.core.json.JsonObject;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class EeaSendTransaction {
  private static final String RESTRICTED = "restricted";
  private static final String UNLOCKED_ACCOUNT = "0x7577919ae5df4941180eac211965f275cdce314d";
  private static final String PRIVATE_FROM = "ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI=";
  private static final List<String> PRIVATE_FOR =
      singletonList("GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w=");
  private static final String VALUE = "0x0";
  private static final String DATA =
      "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675";

  public Request<Object, EthSendTransaction> withGas(final String gas) {
    return createRequest(defaultTransaction().withGas(gas).build());
  }

  public Request<Object, EthSendTransaction> withGasPrice(final String gasPrice) {
    return createRequest(defaultTransaction().withGasPrice(gasPrice).build());
  }

  public Request<Object, EthSendTransaction> withValue(final String value) {
    return createRequest(defaultTransaction().withValue(value).build());
  }

  public Request<Object, EthSendTransaction> withNonce(final String nonce) {
    return createRequest(defaultTransaction().withNonce(nonce).build());
  }

  public Request<Object, EthSendTransaction> withSender(final String sender) {
    return createRequest(defaultTransaction().withFrom(sender).build());
  }

  public Request<Object, EthSendTransaction> withReceiver(final String sender) {
    return createRequest(defaultTransaction().withTo(sender).build());
  }

  public Request<?, EthSendTransaction> withData(final String data) {
    return createRequest(defaultTransaction().withData(data).build());
  }

  public Request<?, EthSendTransaction> missingSender() {
    return createRequest(transactionWithoutField("from"));
  }

  public Request<?, EthSendTransaction> missingNonce() {
    return createRequest(transactionWithoutField("nonce"));
  }

  public Request<?, EthSendTransaction> missingReceiver() {
    return createRequest(transactionWithoutField("to"));
  }

  public Request<?, EthSendTransaction> missingValue() {
    return createRequest(transactionWithoutField("value"));
  }

  public Request<?, EthSendTransaction> missingGas() {
    return createRequest(transactionWithoutField("gas"));
  }

  public Request<?, EthSendTransaction> missingGasPrice() {
    return createRequest(transactionWithoutField("gasPrice"));
  }

  public Request<?, EthSendTransaction> missingData() {
    return createRequest(transactionWithoutField("data"));
  }

  public Request<Object, EthSendTransaction> missingPrivateFrom() {
    return createRequest(transactionWithoutField("privateFrom"));
  }

  public Request<Object, EthSendTransaction> missingPrivateFor() {
    return createRequest(transactionWithoutField("privateFor"));
  }

  public Request<Object, EthSendTransaction> missingRestriction() {
    return createRequest(transactionWithoutField("restriction"));
  }

  /**
   * Due to the underlying server mocking, When only a single request is used, the contents does not
   * actually matter, only their equivalence does.
   */
  public Request<Object, EthSendTransaction> request() {
    return createRequest(defaultTransaction().build());
  }

  public Request<?, EthSendTransaction> smartContract() {
    final PrivateTransactionBuilder transaction =
        new PrivateTransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withGas("0x76c0")
            .withGasPrice("0x9184e72a000")
            .withValue(VALUE)
            .withNonce("0x1")
            .withData(DATA)
            .withPrivateFrom(PRIVATE_FROM)
            .withPrivateFor(PRIVATE_FOR)
            .withRestriction(RESTRICTED);
    return createRequest(transaction.build());
  }

  private PrivateTransactionBuilder defaultTransaction() {
    return new PrivateTransactionBuilder()
        .withFrom(UNLOCKED_ACCOUNT)
        .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
        .withGasPrice("0x9184e72a000")
        .withGas("0x76c0")
        .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
        .withValue(VALUE)
        .withData(DATA)
        .withPrivateFrom(PRIVATE_FROM)
        .withPrivateFor(PRIVATE_FOR)
        .withRestriction(RESTRICTED);
  }

  private JsonObject transactionWithoutField(final String field) {
    final JsonObject transaction = defaultTransaction().build();
    transaction.remove(field);
    return transaction;
  }

  private Request<Object, EthSendTransaction> createRequest(final JsonObject transaction) {
    final Request<Object, EthSendTransaction> eea_sendTransaction =
        new Request<>(
            "eea_sendTransaction", singletonList(transaction), null, EthSendTransaction.class);
    eea_sendTransaction.setId(DEFAULT_ID);
    return eea_sendTransaction;
  }
}

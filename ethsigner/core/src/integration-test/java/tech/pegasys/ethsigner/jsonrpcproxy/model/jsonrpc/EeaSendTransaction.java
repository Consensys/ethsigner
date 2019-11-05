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
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_DATA;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_FROM;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_GAS_PRICE;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_NONCE;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_TO;
import static tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.SendTransaction.FIELD_VALUE;

import java.util.List;

import io.vertx.core.json.JsonObject;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class EeaSendTransaction {
  public static final String FIELD_PRIVATE_FROM = "privateFrom";
  public static final String FIELD_PRIVATE_FOR = "privateFor";
  public static final String FIELD_RESTRICTION = "restriction";

  private static final String UNLOCKED_ACCOUNT = "0x7577919ae5df4941180eac211965f275cdce314d";
  private static final String DEFAULT_VALUE = "0x0";

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

  public Request<?, EthSendTransaction> withPrivateFor(final List<String> privateFor) {
    return createRequest(defaultTransaction().withPrivateFor(privateFor).build());
  }

  public Request<?, EthSendTransaction> withRestriction(final String restriction) {
    return createRequest(defaultTransaction().withRestriction(restriction).build());
  }

  public Request<?, EthSendTransaction> missingSender() {
    return createRequest(transactionWithoutField(FIELD_FROM));
  }

  public Request<?, EthSendTransaction> missingNonce() {
    return createRequest(transactionWithoutField(FIELD_NONCE));
  }

  public Request<?, EthSendTransaction> missingReceiver() {
    return createRequest(transactionWithoutField(FIELD_TO));
  }

  public Request<?, EthSendTransaction> missingValue() {
    return createRequest(transactionWithoutField(FIELD_VALUE));
  }

  public Request<?, EthSendTransaction> missingGas() {
    return createRequest(transactionWithoutField(FIELD_GAS));
  }

  public Request<?, EthSendTransaction> missingGasPrice() {
    return createRequest(transactionWithoutField(FIELD_GAS_PRICE));
  }

  public Request<?, EthSendTransaction> missingData() {
    return createRequest(transactionWithoutField(FIELD_DATA));
  }

  public Request<Object, EthSendTransaction> missingPrivateFrom() {
    return createRequest(transactionWithoutField(FIELD_PRIVATE_FROM));
  }

  public Request<Object, EthSendTransaction> missingPrivateFor() {
    return createRequest(transactionWithoutField(FIELD_PRIVATE_FOR));
  }

  public Request<Object, EthSendTransaction> missingRestriction() {
    return createRequest(transactionWithoutField(FIELD_RESTRICTION));
  }

  /**
   * Due to the underlying server mocking, When only a single request is used, the contents does not
   * actually matter, only their equivalence does.
   */
  public Request<Object, EthSendTransaction> request() {
    return createRequest(defaultTransaction().build());
  }

  private PrivateTransactionBuilder defaultTransaction() {
    return new PrivateTransactionBuilder()
        .withFrom(UNLOCKED_ACCOUNT)
        .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
        .withGasPrice("0x9184e72a000")
        .withGas("0x76c0")
        .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
        .withValue(DEFAULT_VALUE)
        .withData(
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675")
        .withPrivateFrom("ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI=")
        .withPrivateFor(singletonList("GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w="))
        .withRestriction("restricted");
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

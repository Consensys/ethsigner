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

import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.PrivateTransaction.ValueHolder;
import tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc.Transaction.Builder;

import java.util.Optional;

import io.vertx.core.json.JsonObject;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class SendTransaction {
  public static final String FIELD_VALUE_DEFAULT = "0x0";
  public static final String FIELD_GAS_DEFAULT = "0x15F90";
  public static final String FIELD_GAS_PRICE_DEFAULT = "0x0";
  public static final String FIELD_DATA_DEFAULT = "";
  public static final String FIELD_FROM = "from";
  public static final String FIELD_NONCE = "nonce";
  public static final String FIELD_TO = "to";
  public static final String FIELD_VALUE = "value";
  public static final String FIELD_GAS = "gas";
  public static final String FIELD_GAS_PRICE = "gasPrice";
  public static final String FIELD_DATA = "data";

  private static final String UNLOCKED_ACCOUNT = "0x7577919ae5df4941180eac211965f275cdce314d";

  /**
   * Due to the underlying server mocking, When only a single request is used, the contents does not
   * actually matter, only their equivalence does.
   */
  public Request<?, EthSendTransaction> request(final Transaction transaction) {
    final JsonObject jsonObject = new JsonObject();
    putValue(jsonObject, FIELD_FROM, transaction.getFrom());
    putValue(jsonObject, FIELD_NONCE, transaction.getNonce());
    putValue(jsonObject, FIELD_GAS_PRICE, transaction.getGasPrice());
    putValue(jsonObject, FIELD_GAS, transaction.getGas());
    putValue(jsonObject, FIELD_TO, transaction.getTo());
    putValue(jsonObject, FIELD_VALUE, transaction.getValue());
    putValue(jsonObject, FIELD_DATA, transaction.getData());
    return createRequest(jsonObject);
  }

  public Request<?, EthSendTransaction> request(final Transaction.Builder transactionBuilder) {
    return request(transactionBuilder.build());
  }

  public Builder smartContract() {
    return new Builder()
        .withFrom(UNLOCKED_ACCOUNT)
        .withGas("0x76c0")
        .withGasPrice("0x9184e72a000")
        .withValue(FIELD_VALUE_DEFAULT)
        .withNonce("0x1")
        .withData(
            "0x608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a");
  }

  public Builder defaultTransaction() {
    return new Transaction.Builder()
        .withFrom(UNLOCKED_ACCOUNT)
        .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
        .withGasPrice("0x9184e72a000")
        .withGas("0x76c0")
        .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
        .withValue("0x9184e72a")
        .withData(
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");
  }

  private Request<?, EthSendTransaction> createRequest(final JsonObject transaction) {
    final Request<Object, EthSendTransaction> eea_sendTransaction =
        new Request<>(
            "eth_sendTransaction", singletonList(transaction), null, EthSendTransaction.class);
    eea_sendTransaction.setId(DEFAULT_ID);
    return eea_sendTransaction;
  }

  private <T> void putValue(
      final JsonObject jsonObject, final String field, final Optional<ValueHolder<T>> value) {
    value.ifPresent(valueHolder -> jsonObject.put(field, valueHolder.getValue()));
  }
}

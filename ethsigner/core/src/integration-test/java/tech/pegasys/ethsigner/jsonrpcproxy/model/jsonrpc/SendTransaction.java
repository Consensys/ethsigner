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

import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class SendTransaction {

  private static final String UNLOCKED_ACCOUNT = "0x7577919ae5df4941180eac211965f275cdce314d";

  public Request<?, EthSendTransaction> withGas(final String gas) {
    final TransactionBuilder transaction = createDefaultTransaction().withGas(gas);
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> withGasPrice(final String gasPrice) {
    final TransactionBuilder transaction = createDefaultTransaction().withGasPrice(gasPrice);
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> withValue(final String value) {
    final TransactionBuilder transaction = createDefaultTransaction().withValue(value);
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> withNonce(final String nonce) {
    final TransactionBuilder transaction = createDefaultTransaction().withNonce(nonce);
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> withSender(final String sender) {
    final TransactionBuilder transaction = createDefaultTransaction().withFrom(sender);
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> withReceiver(final String receiver) {
    final TransactionBuilder transaction = createDefaultTransaction().withTo(receiver);
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> withData(final String data) {
    final TransactionBuilder transaction = createDefaultTransaction().withData(data);
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> missingSender() {
    final TransactionBuilder transaction = createDefaultTransaction().removeFrom();
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> missingNonce() {
    final TransactionBuilder transaction = createDefaultTransaction().removeNonce();
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> missingReceiver() {
    final TransactionBuilder transaction = createDefaultTransaction().removeTo();
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> missingValue() {
    final TransactionBuilder transaction = createDefaultTransaction().removeValue();
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> missingGas() {
    final TransactionBuilder transaction = createDefaultTransaction().removeGas();
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> missingGasPrice() {
    final TransactionBuilder transaction = createDefaultTransaction().removeGasPrice();
    return createRequest(transaction);
  }

  public Request<?, EthSendTransaction> missingData() {
    final TransactionBuilder transaction = createDefaultTransaction().removeData();
    return createRequest(transaction);
  }

  /**
   * Due to the underlying server mocking, When only a single request is used, the contents does not
   * actually matter, only their equivalence does.
   */
  public Request<?, EthSendTransaction> request() {
    return createRequest(createDefaultTransaction());
  }

  public Request<?, EthSendTransaction> smartContract() {
    // TODO put default values for value, gas, gasPrice in transaction
    final TransactionBuilder transaction =
        new TransactionBuilder()
            .withFrom(UNLOCKED_ACCOUNT)
            .withGas("0x76c0")
            .withGasPrice("0x9184e72a000")
            .withValue("0x0")
            .withNonce("0x1")
            .withData(
                "0x608060405234801561001057600080fd5b50604051602080610114833981016040525160005560e1806100336000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632a1afcd98114605757806360fe47b114607b5780636d4ce63c146092575b600080fd5b348015606257600080fd5b50606960a4565b60408051918252519081900360200190f35b348015608657600080fd5b50609060043560aa565b005b348015609d57600080fd5b50606960af565b60005481565b600055565b600054905600a165627a7a72305820ade758a90b7d6841e99ca64c339eda0498d86ec9a97d5dcdeb3f12e3500079130029000000000000000000000000000000000000000000000000000000000000000a");
    return createRequest(transaction);
  }

  private TransactionBuilder createDefaultTransaction() {
    return new TransactionBuilder()
        .withFrom(UNLOCKED_ACCOUNT)
        .withNonce("0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2")
        .withGasPrice("0x9184e72a000")
        .withGas("0x76c0")
        .withTo("0xd46e8dd67c5d32be8058bb8eb970870f07244567")
        .withValue("0x9184e72a")
        .withData(
            "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");
  }

  private Request<?, EthSendTransaction> createRequest(
      final TransactionBuilder transactionBuilder) {
    final Transaction transaction = transactionBuilder.build();
    final Request<Object, EthSendTransaction> eea_sendTransaction =
        new Request<>(
            "eth_sendTransaction", singletonList(transaction), null, EthSendTransaction.class);
    eea_sendTransaction.setId(DEFAULT_ID);
    return eea_sendTransaction;
  }
}

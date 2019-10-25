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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Optional;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class EthSendTransactionJsonParametersTest {

  private Optional<BigInteger> getStringAsOptionalBigInteger(
      final JsonObject object, final String key) {
    final String value = object.getString(key);
    return Optional.of(new BigInteger(value.substring(2), 16));
  }

  @Test
  void transactionStoredInJsonArrayCanBeDecoded() throws Throwable {
    final JsonObject parameters = validEthTransactionParameters();

    final JsonRpcRequest request = wrapParametersInRequest(parameters);
    final EeaSendTransactionJsonParameters txnParams =
        EeaSendTransactionJsonParameters.from(request);

    assertThat(txnParams.gas()).isEqualTo(getStringAsOptionalBigInteger(parameters, "gas"));
    assertThat(txnParams.gasPrice())
        .isEqualTo(getStringAsOptionalBigInteger(parameters, "gasPrice"));
    assertThat(txnParams.nonce()).isEqualTo(getStringAsOptionalBigInteger(parameters, "nonce"));
    assertThat(txnParams.receiver()).isEqualTo(Optional.of(parameters.getString("to")));
    assertThat(txnParams.value()).isEqualTo(getStringAsOptionalBigInteger(parameters, "value"));
  }

  @Test
  void transactionNotStoredInJsonArrayCanBeDecoded() throws Throwable {
    final JsonObject parameters = validEthTransactionParameters();

    final JsonRpcRequest request = wrapParametersInRequest(parameters);
    final EeaSendTransactionJsonParameters txnParams =
        EeaSendTransactionJsonParameters.from(request);

    assertThat(txnParams.gas()).isEqualTo(getStringAsOptionalBigInteger(parameters, "gas"));
    assertThat(txnParams.gasPrice())
        .isEqualTo(getStringAsOptionalBigInteger(parameters, "gasPrice"));
    assertThat(txnParams.nonce()).isEqualTo(getStringAsOptionalBigInteger(parameters, "nonce"));
    assertThat(txnParams.receiver()).isEqualTo(Optional.of(parameters.getString("to")));
    assertThat(txnParams.value()).isEqualTo(getStringAsOptionalBigInteger(parameters, "value"));
  }

  @Test
  void transactionWithInvalidFromCanBeDecoded() {
    final JsonObject parameters = validEthTransactionParameters();
    parameters.put("from", "invalidFromAddress");

    final JsonRpcRequest request = wrapParametersInRequest(parameters);
    final EeaSendTransactionJsonParameters txnParams =
        EeaSendTransactionJsonParameters.from(request);

    assertThat(txnParams.sender()).isEqualTo("invalidFromAddress");
  }

  @Test
  void transactionWithInvalidToCanBeDecoded() {
    final JsonObject parameters = validEthTransactionParameters();
    parameters.put("to", "invalidToAddress");

    final JsonRpcRequest request = wrapParametersInRequest(parameters);
    final EeaSendTransactionJsonParameters txnParams =
        EeaSendTransactionJsonParameters.from(request);

    assertThat(txnParams.receiver()).contains("invalidToAddress");
  }

  private JsonObject validEthTransactionParameters() {
    final JsonObject parameters = new JsonObject();
    parameters.put("from", "0xb60e8dd61c5d32be8058bb8eb970870f07233155");
    parameters.put("to", "0xd46e8dd67c5d32be8058bb8eb970870f07244567");
    parameters.put("nonce", "0x1");
    parameters.put("gas", "0x76c0");
    parameters.put("gasPrice", "0x9184e72a000");
    parameters.put("value", "0x0");
    parameters.put(
        "data",
        "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    return parameters;
  }

  private <T> JsonRpcRequest wrapParametersInRequest(final T parameters) {
    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", parameters);

    return input.mapTo(JsonRpcRequest.class);
  }
}

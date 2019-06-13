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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class EeaSendTransactionJsonParametersTest {

  private Optional<BigInteger> getStringAsOptionalBigInteger(
      final JsonObject object, final String key) {
    final String value = object.getString(key);
    return Optional.of(new BigInteger(value.substring(2), 16));
  }

  @SuppressWarnings("unchecked")
  private List<Object> getAsArray(final JsonObject parameters, final String key) {
    return parameters.getJsonArray(key).getList();
  }

  @Test
  public void transactionStoredInJsonArrayCanBeDecoded() {
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
    parameters.put("privateFrom", "ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI=");
    parameters.put("privateFor", singletonList("GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w="));
    parameters.put("restriction", "restricted");

    final JsonArray inputParameters = new JsonArray();
    inputParameters.add(parameters);

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", inputParameters);

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);
    final EeaSendTransactionJsonParameters txnParams =
        EeaSendTransactionJsonParameters.from(request);

    assertThat(txnParams.gas()).isEqualTo(getStringAsOptionalBigInteger(parameters, "gas"));
    assertThat(txnParams.gasPrice())
        .isEqualTo(getStringAsOptionalBigInteger(parameters, "gasPrice"));
    assertThat(txnParams.nonce()).isEqualTo(getStringAsOptionalBigInteger(parameters, "nonce"));
    assertThat(txnParams.receiver()).isEqualTo(Optional.of(parameters.getString("to")));
    assertThat(txnParams.value()).isEqualTo(getStringAsOptionalBigInteger(parameters, "value"));
    assertThat(txnParams.privateFrom()).isEqualTo(parameters.getString("privateFrom"));
    assertThat(txnParams.privateFor()).isEqualTo(getAsArray(parameters, "privateFor"));
    assertThat(txnParams.restriction()).isEqualTo(parameters.getString("restriction"));
  }

  @Test
  public void transactionNotStoredInJsonArrayCanBeDecoded() {
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
    parameters.put("privateFrom", "ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI=");
    parameters.put("privateFor", singletonList("GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w="));
    parameters.put("restriction", "restricted");

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", parameters);

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);
    final EeaSendTransactionJsonParameters txnParams =
        EeaSendTransactionJsonParameters.from(request);

    assertThat(txnParams.gas()).isEqualTo(getStringAsOptionalBigInteger(parameters, "gas"));
    assertThat(txnParams.gasPrice())
        .isEqualTo(getStringAsOptionalBigInteger(parameters, "gasPrice"));
    assertThat(txnParams.nonce()).isEqualTo(getStringAsOptionalBigInteger(parameters, "nonce"));
    assertThat(txnParams.receiver()).isEqualTo(Optional.of(parameters.getString("to")));
    assertThat(txnParams.value()).isEqualTo(getStringAsOptionalBigInteger(parameters, "value"));
    assertThat(txnParams.privateFrom()).isEqualTo(parameters.getString("privateFrom"));
    assertThat(txnParams.privateFor()).isEqualTo(getAsArray(parameters, "privateFor"));
    assertThat(txnParams.restriction()).isEqualTo(parameters.getString("restriction"));
  }

  @Test
  public void transactionWithNonZeroValueFails() {
    final JsonObject parameters = new JsonObject();
    parameters.put("from", "0xb60e8dd61c5d32be8058bb8eb970870f07233155");
    parameters.put("to", "0xd46e8dd67c5d32be8058bb8eb970870f07244567");
    parameters.put("nonce", "0x1");
    parameters.put("gas", "0x76c0");
    parameters.put("gasPrice", "0x9184e72a000");
    parameters.put("value", "0x9184e72a");
    parameters.put(
        "data",
        "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", parameters);

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> EeaSendTransactionJsonParameters.from(request));
  }
}

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
package tech.pegasys.ethsigner.jsonrpcproxy;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.requesthandler.internalresponse.EthAccountsBodyProvider;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class EthAccountsBodyProviderTest {

  @Test
  public void valueFromBodyProviderInsertedToResult() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsBodyProvider bodyProvider = new EthAccountsBodyProvider(address);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts", emptyList());
    request.setId(new JsonRpcRequestId(id));

    final JsonRpcBody body = bodyProvider.getBody(request);
    final JsonObject jsonObj = new JsonObject(body.body());

    assertThat(body.hasError()).isFalse();
    assertThat(jsonObj.getString("jsonrpc")).isEqualTo("2.0");
    assertThat(jsonObj.getInteger("id")).isEqualTo(id);
    assertThat(jsonObj.getJsonArray("result")).containsExactly(address);
  }

  @Test
  public void ifParamsContainsANonEmptyArrayErrorIsReturned() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsBodyProvider bodyProvider = new EthAccountsBodyProvider(address);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts", singletonList(5));
    request.setId(new JsonRpcRequestId(id));

    final JsonRpcBody body = bodyProvider.getBody(request);
    final JsonObject response = new JsonObject(Json.encodeToBuffer(body.error()));

    assertThat(body.hasError()).isTrue();
    assertThat(response.getString("jsonrpc")).isEqualTo("2.0");
    assertThat(response.getValue("id")).isEqualTo(id);
    final JsonObject error = response.getJsonObject("error");
    assertThat(error.getInteger("code")).isEqualTo(JsonRpcError.INVALID_PARAMS.getCode());
    assertThat(error.getString("message")).isEqualTo(JsonRpcError.INVALID_PARAMS.getMessage());
  }

  @Test
  public void isParamIsAnObjectErrorIsReturned() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsBodyProvider bodyProvider = new EthAccountsBodyProvider(address);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts", 5);
    request.setId(new JsonRpcRequestId(id));

    final JsonRpcBody body = bodyProvider.getBody(request);
    final JsonObject response = new JsonObject(Json.encodeToBuffer(body.error()));

    assertThat(body.hasError()).isTrue();
    assertThat(response.getString("jsonrpc")).isEqualTo("2.0");
    assertThat(response.getValue("id")).isEqualTo(id);
    final JsonObject error = response.getJsonObject("error");
    assertThat(error.getInteger("code")).isEqualTo(JsonRpcError.INVALID_PARAMS.getCode());
    assertThat(error.getString("message")).isEqualTo(JsonRpcError.INVALID_PARAMS.getMessage());
  }

  @Test
  public void missingParametersIsOk() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsBodyProvider bodyProvider = new EthAccountsBodyProvider(address);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts", null);
    request.setId(new JsonRpcRequestId(id));

    final JsonRpcBody body = bodyProvider.getBody(request);
    final JsonObject jsonObj = new JsonObject(body.body());

    assertThat(body.hasError()).isFalse();
    assertThat(jsonObj.getString("jsonrpc")).isEqualTo("2.0");
    assertThat(jsonObj.getInteger("id")).isEqualTo(id);
    assertThat(jsonObj.getJsonArray("result")).containsExactly(address);
  }
}

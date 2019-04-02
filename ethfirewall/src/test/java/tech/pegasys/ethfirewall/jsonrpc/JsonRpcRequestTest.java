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
package tech.pegasys.ethfirewall.jsonrpc;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.Lists;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class JsonRpcRequestTest {

  @Test
  public void basicDecoding() {

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", 5);

    final JsonRpcRequest request = JsonRpcRequest.convertFrom(input);
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
    assertThat(request.getParams()).isEqualTo(5);
  }

  @Test
  public void arrayOfParamsDecodesIntoSingleObject() {

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", singletonList(5));

    final JsonRpcRequest request = JsonRpcRequest.convertFrom(input);
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
    assertThat(request.getParams()).isEqualTo(5);
  }

  @Test
  public void arrayOfSingleJsonObjectCorrectExtracts() {
    final JsonObject parameters = new JsonObject("{\"input\":\"yes\"}");

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", singletonList(parameters));

    final JsonRpcRequest request = JsonRpcRequest.convertFrom(input);
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
    assertThat(request.getParams()).isEqualTo(parameters);
  }

  @Test
  public void multiEntriesInParamsThrowsException() {
    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", Lists.newArrayList(5, 5));

    assertThatThrownBy(() -> JsonRpcRequest.convertFrom(input))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void emptyParametersIsValid() {
    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", emptyList());

    final JsonRpcRequest request = JsonRpcRequest.convertFrom(input);
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
    assertThat(request.getParams()).isNull();
  }
}

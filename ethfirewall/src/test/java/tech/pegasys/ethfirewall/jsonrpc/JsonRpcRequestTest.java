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

import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class JsonRpcRequestTest {

  @Test
  public void basicDecoding() {

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", 5);

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);
  }
}

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

import java.util.Map;

import io.vertx.core.json.Json;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

public class EthNodeRequest {

  private final String body;
  private final Map<String, String> headers;

  public EthNodeRequest(
      final Map<String, String> headers, final Request<?, ? extends Response<?>> body) {
    this.body = Json.encode(body);
    this.headers = headers;
  }

  public String getBody() {
    return body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }
}

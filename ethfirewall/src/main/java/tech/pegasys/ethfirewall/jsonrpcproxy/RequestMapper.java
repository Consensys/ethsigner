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
package tech.pegasys.ethfirewall.jsonrpcproxy;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.json.JsonObject;

public class RequestMapper {
  private final RequestHandler defaultHandler;
  private final Map<String, RequestHandler> handlers = new HashMap<>();

  public RequestMapper(final RequestHandler defaultHandler) {
    this.defaultHandler = defaultHandler;
  }

  public void addHandler(final String jsonMethod, final RequestHandler requestHandler) {
    handlers.put(jsonMethod, requestHandler);
  }

  public RequestHandler getMatchingHandler(final JsonObject bodyJson) {
    final String method = bodyJson.getString("method");
    return handlers.getOrDefault(method, defaultHandler);
  }
}

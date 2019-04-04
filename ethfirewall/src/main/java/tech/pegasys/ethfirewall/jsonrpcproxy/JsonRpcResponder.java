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

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class JsonRpcResponder {

  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();

  public void populateResponse(
      final HttpServerRequest httpRequest, final int statusCode, final Buffer body) {
    final HttpServerResponse response = httpRequest.response();

    response.putHeader("Content", JSON);
    response.setStatusCode(statusCode);
    response.setChunked(false);
    response.end(body);
  }
}

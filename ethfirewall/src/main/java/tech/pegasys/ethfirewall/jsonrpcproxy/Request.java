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

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;

public class Request {
  private final String uri;
  private final HttpMethod method;
  private final Buffer body;
  private final MultiMap headers;

  public Request(
      final String uri, final HttpMethod method, final Buffer body, final MultiMap headers) {
    this.uri = uri;
    this.method = method;
    this.body = body;
    this.headers = headers;
  }

  public String getUri() {
    return uri;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public Buffer getBody() {
    return body;
  }

  public MultiMap getHeaders() {
    return headers;
  }
}

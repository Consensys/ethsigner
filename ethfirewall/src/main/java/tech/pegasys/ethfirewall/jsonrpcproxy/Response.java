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

public class Response {
  private int statusCode;
  private MultiMap headers;
  private Buffer body;

  public Response(final int statusCode, final MultiMap headers, final Buffer body) {
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public MultiMap getHeaders() {
    return headers;
  }

  public Buffer getBody() {
    return body;
  }

  public void setStatusCode(final int statusCode) {
    this.statusCode = statusCode;
  }

  public void setHeaders(final MultiMap headers) {
    this.headers = headers;
  }

  public void setBody(final Buffer body) {
    this.body = body;
  }
}

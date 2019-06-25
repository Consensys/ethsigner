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
package tech.pegasys.ethsigner.tests.dsl.http;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpResponse {

  private final HttpResponseStatus code;
  private final String body;

  public HttpResponse(final int code, final String body) {
    this.code = HttpResponseStatus.valueOf(code);
    this.body = body;
  }

  public HttpResponseStatus status() {
    return code;
  }

  public String body() {
    return body;
  }
}

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

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpRequest {

  private final OkHttpClient client;
  private final String contextRoot;

  public HttpRequest(final String contextRoot, final OkHttpClient client) {
    this.contextRoot = contextRoot;
    this.client = client;
  }

  public HttpResponse get(final String path) {
    try {
      final okhttp3.Response reply =
          client.newCall(new Request.Builder().url(contextRoot + path).get().build()).execute();
      return new HttpResponse(reply.code(), reply.body().string());
    } catch (final IOException e) {
      throw new RuntimeException("Get request has failed", e);
    }
  }
}

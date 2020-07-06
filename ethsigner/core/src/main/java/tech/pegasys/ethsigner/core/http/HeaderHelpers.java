/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.core.http;

import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import java.util.HashMap;
import java.util.Map;

import com.google.common.net.HttpHeaders;
import io.vertx.core.MultiMap;

public class HeaderHelpers {

  public static Map<String, String> createHeaders(final MultiMap headers) {
    final MultiMap subset = new VertxHttpHeaders();
    headers.forEach(entry -> subset.add(entry.getKey(), entry.getValue()));

    subset.remove(HttpHeaders.CONTENT_LENGTH);
    subset.remove(HttpHeaders.ORIGIN);
    renameHeader(subset, HttpHeaders.HOST, HttpHeaders.X_FORWARDED_HOST);

    final Map<String, String> headersToSend = new HashMap<>();
    subset.forEach(entry -> headersToSend.put(entry.getKey(), entry.getValue()));
    return headersToSend;
  }

  private static void renameHeader(
      final MultiMap headers, final String oldHeader, final String newHeader) {
    final String oldHeaderValue = headers.get(oldHeader);
    headers.remove(oldHeader);
    if (oldHeaderValue != null) {
      headers.add(newHeader, oldHeaderValue);
    }
  }
}

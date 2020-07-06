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

import java.util.HashMap;
import java.util.Map;

import com.google.common.net.HttpHeaders;
import io.vertx.core.MultiMap;

public class HeaderHelpers {

  public static Map<String, String> createHeaders(final MultiMap headers) {
    final Map<String, String> headersToSend = new HashMap<>();
    headers.forEach(entry -> headersToSend.put(entry.getKey(), entry.getValue()));
    headersToSend.remove(HttpHeaders.CONTENT_LENGTH);
    headersToSend.remove(HttpHeaders.ORIGIN);
    renameHeader(headersToSend, HttpHeaders.HOST, HttpHeaders.X_FORWARDED_HOST);
    return headersToSend;
  }

  public static void renameHeader(
      final Map<String, String> headers, final String oldHeader, final String newHeader) {
    final String oldHeaderValue = headers.get(oldHeader);
    headers.remove(oldHeader);
    if (oldHeaderValue != null) {
      headers.put(newHeader, oldHeaderValue);
    }
  }
}

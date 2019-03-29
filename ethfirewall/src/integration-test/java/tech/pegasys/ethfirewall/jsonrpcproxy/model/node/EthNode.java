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
package tech.pegasys.ethfirewall.jsonrpcproxy.model.node;

import static java.util.Collections.emptyMap;

import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;

public class EthNode {

  private static final Map<String, String> NO_HEADERS = emptyMap();

  public EthNodeResponse response(final Map<String, String> headers, final String body) {
    return new EthNodeResponse(headers, body, HttpResponseStatus.OK);
  }

  public EthNodeResponse response(final String body) {
    return new EthNodeResponse(NO_HEADERS, body, HttpResponseStatus.OK);
  }

  public EthNodeResponse response(final String body, final HttpResponseStatus code) {
    return new EthNodeResponse(NO_HEADERS, body, code);
  }

  public EthNodeRequest request(final String body) {
    return new EthNodeRequest(NO_HEADERS, body);
  }
}

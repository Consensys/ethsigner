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
package tech.pegasys.ethfirewall.jsonrpcproxy.model.response;

import static java.util.Collections.emptyMap;

import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;

import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;

public class EthResponseFactory {

  private static final Map<String, String> NO_HEADERS = emptyMap();
  private static final int DEFAULT_ID = 77;

  public EthFirewallResponse ethFirewall(final Object id, final JsonRpcError error) {
    return new EthFirewallResponse(
        NO_HEADERS, new JsonRpcErrorResponse(id, error), HttpResponseStatus.BAD_REQUEST);
  }

  public EthFirewallResponse ethFirewall(final JsonRpcError error) {
    return new EthFirewallResponse(
        NO_HEADERS, new JsonRpcErrorResponse(DEFAULT_ID, error), HttpResponseStatus.BAD_REQUEST);
  }

  public EthFirewallResponse ethFirewall(final Map<String, String> headers, final String body) {
    return new EthFirewallResponse(headers, body, HttpResponseStatus.OK);
  }

  public EthFirewallResponse ethFirewall(final String body) {
    return new EthFirewallResponse(NO_HEADERS, body, HttpResponseStatus.OK);
  }

  public EthFirewallResponse ethFirewall(final String body, final HttpResponseStatus code) {
    return new EthFirewallResponse(NO_HEADERS, body, code);
  }

  public EthNodeResponse ethNode(final Map<String, String> headers, final String body) {
    return new EthNodeResponse(headers, body, HttpResponseStatus.OK);
  }

  public EthNodeResponse ethNode(final String body) {
    return new EthNodeResponse(NO_HEADERS, body, HttpResponseStatus.OK);
  }

  public EthNodeResponse ethNode(final String body, final HttpResponseStatus code) {
    return new EthNodeResponse(NO_HEADERS, body, code);
  }
}

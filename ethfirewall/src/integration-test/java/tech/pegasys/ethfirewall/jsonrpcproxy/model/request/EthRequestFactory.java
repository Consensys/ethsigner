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
package tech.pegasys.ethfirewall.jsonrpcproxy.model.request;

import static java.util.Collections.emptyMap;

import java.util.Map;

public class EthRequestFactory {

  private static final Map<String, String> NO_HEADERS = emptyMap();

  public EthFirewallRequest ethFirewall(final Map<String, String> headers, final String body) {
    return new EthFirewallRequest(headers, body);
  }

  public EthFirewallRequest ethFirewall(final String body) {
    return new EthFirewallRequest(NO_HEADERS, body);
  }

  public EthNodeRequest ethNode(final String body) {
    return new EthNodeRequest(NO_HEADERS, body);
  }
}

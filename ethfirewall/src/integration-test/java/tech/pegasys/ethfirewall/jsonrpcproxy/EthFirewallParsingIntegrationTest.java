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

import static tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError.PARSE_ERROR;

import org.junit.Test;

public class EthFirewallParsingIntegrationTest extends IntegrationTestBase {

  private static final Object NO_ID = null;

  @Test
  public void parseErrorResponseWhenJsonRequestIsMalformed() {
    sendRequestThenVerifyResponse(
        ethFirewall.request(MALFORMED_JSON), ethFirewall.response(NO_ID, PARSE_ERROR));
  }
}

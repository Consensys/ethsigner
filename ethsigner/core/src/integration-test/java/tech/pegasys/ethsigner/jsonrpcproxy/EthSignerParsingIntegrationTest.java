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
package tech.pegasys.ethsigner.jsonrpcproxy;

import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.PARSE_ERROR;

import org.junit.jupiter.api.Test;

class EthSignerParsingIntegrationTest extends DefaultTestBase {

  private static final Object NO_ID = null;

  @Test
  void parseErrorResponseWhenJsonRequestIsMalformed() {
    sendPostRequestAndVerifyResponse(
        request.ethSigner(MALFORMED_JSON), response.ethSigner(NO_ID, PARSE_ERROR));
  }
}

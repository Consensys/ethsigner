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
package tech.pegasys.ethsigner.tests;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.io.IOException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpClientResponse;
import org.junit.Test;

public class MethodNotFoundAcceptanceTests extends AcceptanceTestBase {

  private volatile HttpClientResponse response = null;

  @Test
  public void sendDisabledApiReturnsBadRequest() throws IOException {
    final SignerResponse<JsonRpcErrorResponse> response =
        ethSigner().rawRequest().exceptionalRequest("ibft_getPendingVotes");

    assertThat(response.status()).isEqualTo(HttpResponseStatus.BAD_REQUEST);
    assertThat(response.jsonRpc().getError()).isEqualTo(JsonRpcError.METHOD_NOT_FOUND);
  }

  @Test
  public void unknownJsonRpcMethodReturnsBadRequest() throws IOException {
    final SignerResponse<JsonRpcErrorResponse> response =
        ethSigner().rawRequest().exceptionalRequest("invalidJsonRpcMethod");

    assertThat(response.status()).isEqualTo(HttpResponseStatus.BAD_REQUEST);
    assertThat(response.jsonRpc().getError()).isEqualTo(JsonRpcError.METHOD_NOT_FOUND);
  }
}

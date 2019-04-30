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

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;

import java.util.Collections;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisabledApi extends AcceptanceTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(DisabledApi.class);

  private volatile HttpClientResponse response = null;

  @Test
  public void sendDisabledApiReturnsBadRequest() {
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "ibft_getPendingVotes", new String[0]);
    request.setId(new JsonRpcRequestId(1));

    ethSigner()
        .sendRawJsonRpc(
            Collections.emptyMap(),
            Json.encodeToBuffer(request),
            response -> this.response = response);

    WaitUtils.waitFor(
        1,
        () -> assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.BAD_REQUEST.code()));
  }

  @Test
  public void unrecognisedJsonRpcMethodReturnsBadRequest() {
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "invalidJsonRpcMethod", new String[0]);
    request.setId(new JsonRpcRequestId(1));

    ethSigner()
        .sendRawJsonRpc(
            Collections.emptyMap(),
            Json.encodeToBuffer(request),
            response -> this.response = response);
    WaitUtils.waitFor(
        1,
        () -> assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.BAD_REQUEST.code()));
  }

  @Test
  public void invalidCorsRequestReportsA403() {
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_blockNumber", new String[0]);
    ethSigner()
        .sendRawJsonRpc(
            singletonMap("origin", "google.com"),
            Json.encodeToBuffer(request),
            response -> this.response = response);

    WaitUtils.waitFor(
        1, () -> assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.FORBIDDEN.code()));
  }
}

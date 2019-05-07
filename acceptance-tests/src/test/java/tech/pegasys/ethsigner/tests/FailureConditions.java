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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.io.IOException;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.exceptions.ClientConnectionException;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;

import java.util.Collections;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import org.junit.Test;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.RawJsonRpcRequestFactory.ArbitraryResponseType;

public class FailureConditions extends AcceptanceTestBase {

  private volatile HttpClientResponse response = null;

  @Test
  public void sendDisabledApiReturnsBadRequest() throws IOException  {
/*
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "invalidJsonRpcMethod", new String[0]);

    JsonRpcErrorResponse
        response = ethSigner().request().submitExceptionalRequest(header, jsonRpcRequest);
    assertThat(Response.error).isEqualTo("blah");
*/
    final Request<?, ArbitraryResponseType> request =
        ethSigner().createRequest(emptyMap(), "ibft_getPendingVotes");


    assertThatExceptionOfType(ClientConnectionException.class).isThrownBy(request::send).

    try {
      request.send();
    }catch(final Throwable t) {
     int i  =5;
      }

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "ibft_getPendingVotes", new String[0]);
    request.setId(new JsonRpcRequestId(1));

    Request<?, ArbitraryResponseType>

    ethSigner()
        .sendRawJsonRpc(
            emptyMap(),
            Json.encodeToBuffer(request),
            response -> this.response = response);

    WaitUtils.waitFor(
        1,
        () -> assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.BAD_REQUEST.code()));
        */
  }

  @Test
  public void unknownJsonRpcMethodReturnsBadRequest() {
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "invalidJsonRpcMethod", new String[0]);
    request.setId(new JsonRpcRequestId(1));

    ethSigner()
        .sendRawJsonRpc(
            emptyMap(),
            Json.encodeToBuffer(request),
            response -> this.response = response);
    WaitUtils.waitFor(
        1,
        () -> assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.BAD_REQUEST.code()));
  }

  /*

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

  */
}

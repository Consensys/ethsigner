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
package tech.pegasys.ethsigner.http;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class JsonRpcErrorHandler implements Handler<RoutingContext> {
  private HttpResponseFactory httpResponseFactory;

  public JsonRpcErrorHandler(HttpResponseFactory httpResponseFactory) {
    this.httpResponseFactory = httpResponseFactory;
  }

  @Override
  public void handle(final RoutingContext context) {
    JsonRpcRequestId rpcRequestId = jsonRpcId(context);
    final JsonRpcErrorResponse errorResponse =
        new JsonRpcErrorResponse(rpcRequestId, INTERNAL_ERROR);
    final int statusCode = context.statusCode() == -1 ? BAD_REQUEST.code() : context.statusCode();
    httpResponseFactory.create(context.request(), statusCode, errorResponse);
  }

  private JsonRpcRequestId jsonRpcId(final RoutingContext context) {
    try {
      final JsonRpcRequest jsonRpcRequest =
          Json.decodeValue(context.getBodyAsString(), JsonRpcRequest.class);
      return jsonRpcRequest.getId();
    } catch (DecodeException e) {
      // TODO should this be logged?
      return new JsonRpcRequestId(null);
    }
  }
}

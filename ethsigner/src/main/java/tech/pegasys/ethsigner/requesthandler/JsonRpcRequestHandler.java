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
package tech.pegasys.ethsigner.requesthandler;

import tech.pegasys.ethsigner.http.HttpResponseFactory;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JsonRpcRequestHandler {

  public abstract void handle(HttpServerRequest httpServerRequest, JsonRpcRequest rpcRequest);

  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcRequestHandler.class);

  protected final HttpResponseFactory responder;

  public JsonRpcRequestHandler(HttpResponseFactory responder) {
    this.responder = responder;
  }

  protected void reportError(
      final HttpServerRequest httpServerRequest,
      final JsonRpcRequest request,
      final JsonRpcError errorCode) {
    final JsonRpcErrorResponse errorResponse = new JsonRpcErrorResponse(request.getId(), errorCode);

    LOG.debug(
        "Failed to correctly handle request. method: {}, uri: {}, body: {}, Error body: {}",
        httpServerRequest.method(),
        httpServerRequest.absoluteURI(),
        Json.encodePrettily(request),
        Json.encode(errorResponse));

    responder.create(httpServerRequest, HttpResponseStatus.BAD_REQUEST.code(), errorResponse);
  }
}

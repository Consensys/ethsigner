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

import static io.netty.handler.codec.rtsp.RtspResponseStatuses.INTERNAL_SERVER_ERROR;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcErrorHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(LogErrorHandler.class);
  private HttpResponseFactory httpResponseFactory;

  public JsonRpcErrorHandler(HttpResponseFactory httpResponseFactory) {
    this.httpResponseFactory = httpResponseFactory;
  }

  @Override
  public void handle(final RoutingContext context) {
    final Optional<JsonRpcRequest> jsonRpcRequest = jsonRpcRequest(context);
    final JsonRpcErrorResponse errorResponse = errorResponse(context, jsonRpcRequest);
    final int statusCode =
        context.statusCode() == -1 ? INTERNAL_SERVER_ERROR.code() : context.statusCode();
    final String body = jsonRpcRequest.map(Json::encodePrettily).orElse(context.getBodyAsString());
    LOG.debug(
        "Failed to correctly handle request. method: {}, uri: {}, body: {}, Error body: {}",
        context.request().method(),
        context.request().absoluteURI(),
        body,
        Json.encode(errorResponse));
    httpResponseFactory.create(context.request(), statusCode, errorResponse);
  }

  private Optional<JsonRpcRequest> jsonRpcRequest(final RoutingContext context) {
    try {
      return Optional.of(Json.decodeValue(context.getBodyAsString(), JsonRpcRequest.class));
    } catch (DecodeException e) {
      LOG.debug("Parsing body as JSON failed for: {}", context.getBodyAsString(), e);
      return Optional.empty();
    }
  }

  private JsonRpcErrorResponse errorResponse(
      final RoutingContext context, final Optional<JsonRpcRequest> jsonRpcRequest) {
    final JsonRpcRequestId rpcRequestId =
        jsonRpcRequest.map(JsonRpcRequest::getId).orElse(new JsonRpcRequestId(null));
    final JsonRpcError jsonRpcError = jsonRpcError(context);
    return new JsonRpcErrorResponse(rpcRequestId, jsonRpcError);
  }

  private JsonRpcError jsonRpcError(final RoutingContext context) {
    if (context.failure() instanceof JsonRpcException) {
      JsonRpcException jsonRpcException = (JsonRpcException) context.failure();
      return jsonRpcException.getJsonRpcError();
    }
    return INTERNAL_ERROR;
  }
}

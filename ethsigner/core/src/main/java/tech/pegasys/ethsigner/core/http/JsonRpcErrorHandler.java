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
package tech.pegasys.ethsigner.core.http;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.FAILED_TO_CONNECT_TO_DOWNSTREAM_NODE;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonRpcErrorHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LogManager.getLogger();
  private final HttpResponseFactory httpResponseFactory;
  private final JsonDecoder jsonDecoder;

  public JsonRpcErrorHandler(
      final HttpResponseFactory httpResponseFactory, JsonDecoder jsonDecoder) {
    this.httpResponseFactory = httpResponseFactory;
    this.jsonDecoder = jsonDecoder;
  }

  @Override
  public void handle(final RoutingContext context) {
    final Optional<JsonRpcRequest> jsonRpcRequest = jsonRpcRequest(context);
    final JsonRpcErrorResponse errorResponse = errorResponse(context, jsonRpcRequest);
    final int statusCode =
        context.statusCode() == -1 ? INTERNAL_SERVER_ERROR.code() : context.statusCode();
    LOG.debug(
        "Failed to correctly handle request. method: {}, uri: {}, body: {}, Error body: {}",
        context.request()::method,
        context.request()::absoluteURI,
        () -> jsonRpcRequest.map(Json::encodePrettily).orElse(context.getBodyAsString()),
        () -> Json.encode(errorResponse),
        () -> context.failure());
    httpResponseFactory.create(context.request(), statusCode, errorResponse);
  }

  private Optional<JsonRpcRequest> jsonRpcRequest(final RoutingContext context) {
    try {
      return Optional.of(jsonDecoder.decodeValue(context.getBody(), JsonRpcRequest.class));
    } catch (final DecodeException e) {
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
      final JsonRpcException jsonRpcException = (JsonRpcException) context.failure();
      return jsonRpcException.getJsonRpcError();
    } // in case of a timeout we may not have a failure exception so we use the status code
    else if (context.statusCode() == BAD_GATEWAY.code()) {
      return FAILED_TO_CONNECT_TO_DOWNSTREAM_NODE;
    } else if (context.statusCode() == GATEWAY_TIMEOUT.code()) {
      return CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT;
    } else {
      return INTERNAL_ERROR;
    }
  }
}

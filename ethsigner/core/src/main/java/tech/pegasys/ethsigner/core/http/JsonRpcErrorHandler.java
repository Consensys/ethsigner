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

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class JsonRpcErrorHandler implements Handler<RoutingContext> {

  private final HttpResponseFactory httpResponseFactory;

  public JsonRpcErrorHandler(final HttpResponseFactory httpResponseFactory) {
    this.httpResponseFactory = httpResponseFactory;
  }

  @Override
  public void handle(final RoutingContext context) {
    if (context.failure() == null) {
      context
          .response()
          .setStatusCode(
              context.statusCode() != -1 ? context.statusCode() : INTERNAL_SERVER_ERROR.code());
      context.response().end();
    } else {
      if (context.failure() instanceof JsonRpcException) {
        final JsonRpcException exception = (JsonRpcException) context.failure();
        final int statuscode =
            context.statusCode() != -1 ? context.statusCode() : BAD_REQUEST.code();
        httpResponseFactory.create(
            context.request(), statuscode, exception.getJsonRpcErrorResponse());
      } else {
        final int statuscode =
            context.statusCode() != -1 ? context.statusCode() : INTERNAL_SERVER_ERROR.code();
        httpResponseFactory.create(
            context.request(),
            statuscode,
            new JsonRpcErrorResponse(context.get("JsonRpcId"), INTERNAL_ERROR));
      }
    }
  }
}

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
package tech.pegasys.ethsigner.core.requesthandler.internalresponse;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcRequestHandler;
import tech.pegasys.ethsigner.core.requesthandler.ResultProvider;

import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InternalResponseHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpResponseFactory responder;
  private final ResultProvider responseResultProvider;
  private JsonDecoder jsonDecoder;

  public InternalResponseHandler(
      final HttpResponseFactory responder,
      final ResultProvider responseResultProvider,
      final JsonDecoder jsonDecoder) {
    this.responder = responder;
    this.responseResultProvider = responseResultProvider;
    this.jsonDecoder = jsonDecoder;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest rpcRequest) {
    LOG.debug("Internally responding to {}, id={}", rpcRequest.getMethod(), rpcRequest.getId());
    try {
      final Object result = responseResultProvider.createResponseResult(rpcRequest);
      responder.successResponse(context.response(), rpcRequest.getId(), result);
    } catch (final JsonRpcException e) {
      responder.failureResponse(
          context.response(), rpcRequest.getId(), BAD_REQUEST.code(), e.getJsonRpcError());
    }
  }
}

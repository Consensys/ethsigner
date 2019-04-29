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
package tech.pegasys.ethsigner.requesthandler.internalresponse;

import tech.pegasys.ethsigner.http.HttpResponseFactory;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcSuccessResponse;
import tech.pegasys.ethsigner.requesthandler.BodyProvider;
import tech.pegasys.ethsigner.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.requesthandler.JsonRpcRequestHandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalResponseHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(InternalResponseHandler.class);

  private final HttpResponseFactory responder;
  private final BodyProvider responseBodyProvider;

  public InternalResponseHandler(
      final HttpResponseFactory responder, final BodyProvider responseBodyProvider) {
    this.responder = responder;
    this.responseBodyProvider = responseBodyProvider;
  }

  @Override
  public void handle(final RoutingContext routingContext, final JsonRpcRequest rpcRequest) {
    LOG.debug("Internally responding to {}, id={}", rpcRequest.getMethod(), rpcRequest.getId());
    final JsonRpcBody providedBody = responseBodyProvider.getBody(rpcRequest);

    if (providedBody.hasError()) {
      routingContext.fail(new JsonRpcException(providedBody.error()));
    } else {
      final JsonRpcSuccessResponse result =
          Json.decodeValue(providedBody.body(), JsonRpcSuccessResponse.class);
      responder.create(routingContext.request(), HttpResponseStatus.OK.code(), result);
    }
  }
}

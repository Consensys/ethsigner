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

import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcSuccessResponse;
import tech.pegasys.ethsigner.core.requesthandler.BodyProvider;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcRequestHandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InternalResponseHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpResponseFactory responder;
  private final BodyProvider responseBodyProvider;
  private JsonDecoder jsonDecoder;

  public InternalResponseHandler(
      final HttpResponseFactory responder,
      final BodyProvider responseBodyProvider,
      final JsonDecoder jsonDecoder) {
    this.responder = responder;
    this.responseBodyProvider = responseBodyProvider;
    this.jsonDecoder = jsonDecoder;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest rpcRequest) {
    LOG.debug("Internally responding to {}, id={}", rpcRequest.getMethod(), rpcRequest.getId());
    final JsonRpcBody providedBody = responseBodyProvider.getBody(rpcRequest);

    if (providedBody.hasError()) {
      context.fail(new JsonRpcException(providedBody.error()));
    } else {
      final JsonRpcSuccessResponse result =
          jsonDecoder.decodeValue(providedBody.body(), JsonRpcSuccessResponse.class);
      responder.create(context.request(), HttpResponseStatus.OK.code(), result);
    }
  }
}

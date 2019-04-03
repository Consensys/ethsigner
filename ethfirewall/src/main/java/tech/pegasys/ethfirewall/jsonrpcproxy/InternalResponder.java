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
package tech.pegasys.ethfirewall.jsonrpcproxy;

import tech.pegasys.ethfirewall.jsonrpc.JsonRpcRequest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalResponder extends AbstractJsonRpcHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PassThroughHandler.class);

  private final BodyProvider responseBodyProvider;

  public InternalResponder(
      final JsonRpcResponder responder, final BodyProvider responseBodyProvider) {
    super(responder);
    this.responseBodyProvider = responseBodyProvider;
  }

  @Override
  public void handle(final HttpServerRequest httpServerRequest, final JsonRpcRequest rpcRequest) {
    LOG.debug("Internally responding to {}, id={}", rpcRequest.getMethod(), rpcRequest.getId());
    final JsonRpcBody providedBody = responseBodyProvider.getBody(rpcRequest);

    if (providedBody.hasError()) {
      sendErrorResponse(httpServerRequest, providedBody.error());
    } else {
      responder.populateResponse(
          httpServerRequest, HttpResponseStatus.OK.code(), providedBody.body());
    }
  }
}

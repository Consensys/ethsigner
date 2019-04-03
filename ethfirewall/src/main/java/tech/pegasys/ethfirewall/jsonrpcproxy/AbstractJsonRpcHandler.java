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

import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJsonRpcHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractJsonRpcHandler.class);

  protected final JsonRpcResponder responder;

  AbstractJsonRpcHandler(final JsonRpcResponder responder) {
    this.responder = responder;
  }

  void sendErrorResponse(final HttpServerRequest httpRequest, final JsonRpcErrorResponse error) {
    LOG.info("Dropping request from {}", httpRequest.remoteAddress());
    httpRequest.bodyHandler(
        body -> {
          LOG.debug(
              "Dropping request method: {}, uri: {}, body: {}, Error body: {}",
              httpRequest.method(),
              httpRequest.absoluteURI(),
              body.toString(),
              Json.encode(error));
        });

    responder.populateResponse(
        httpRequest, HttpResponseStatus.BAD_REQUEST.code(), Json.encodeToBuffer(error));
  }
}

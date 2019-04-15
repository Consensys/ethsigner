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
package tech.pegasys.ethsigner.jsonrpcproxy;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcErrorReporter {

  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcErrorReporter.class);

  private final HttpResponseFactory responder;

  public JsonRpcErrorReporter(final HttpResponseFactory responder) {
    this.responder = responder;
  }

  void send(
      final JsonRpcRequest jsonRequest,
      final HttpServerRequest httpRequest,
      final JsonRpcErrorResponse error) {
    LOG.info("Error encountered by request from: {}", httpRequest.remoteAddress());
    LOG.debug(
        "Error response from request with method: {}, uri: {}, body: {}, Error body: {}",
        httpRequest.method(),
        httpRequest.absoluteURI(),
        Json.encodePrettily(jsonRequest),
        Json.encode(error));

    responder.create(httpRequest, HttpResponseStatus.BAD_REQUEST.code(), error);
  }
}

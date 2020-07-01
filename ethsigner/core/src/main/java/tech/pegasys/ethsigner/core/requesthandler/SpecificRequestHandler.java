/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.core.requesthandler;

import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter.ResponseBodyHandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class SpecificRequestHandler implements ResponseBodyHandler {

  private final RoutingContext context;
  private final VertxRequestTransmitterFactory transmitterFactory;

  public SpecificRequestHandler(
      final RoutingContext context, final VertxRequestTransmitterFactory transmitterFactory) {
    this.context = context;
    this.transmitterFactory = transmitterFactory;
  }

  public void execute() {
    VertxRequestTransmitter transmitter = transmitterFactory.create(this);
    final HttpServerRequest request = context.request();
    transmitter.sendRequest(context.getBody(), request.path(), request.method(), request.headers());
  }

  @Override
  public void handleResponseBody(final HttpClientResponse response, final Buffer body) {
    context.request().response().setStatusCode(response.statusCode());
    context.request().response().headers().addAll(response.headers());
    context.request().response().setChunked(false);
    context.request().response().end(body);
  }

  @Override
  public void handleTransmissionFailure(final HttpResponseStatus status, final Throwable t) {
    context.fail(status.code(), t);
  }
}

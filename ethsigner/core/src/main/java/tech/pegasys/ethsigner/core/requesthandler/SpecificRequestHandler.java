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

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.RequestForwarder;

public class SpecificRequestHandler extends RequestForwarder {

  private final VertxRequestTransmitter transmitter;

  public SpecificRequestHandler(
      final RoutingContext context, final VertxRequestTransmitterFactory transmitterFactory) {
    super(context);
    transmitter = transmitterFactory.create(this);
  }

  public void send() {
    final HttpServerRequest request = context().request();
    final Map<String, String> headersToSend = createHeaders(request.headers());
    transmitter.postRequest(headersToSend, request.path(), context().getBodyAsString());
  }
}

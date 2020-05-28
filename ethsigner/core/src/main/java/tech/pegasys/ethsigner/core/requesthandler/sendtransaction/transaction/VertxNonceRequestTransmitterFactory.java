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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.DownstreamPathCalculator;

import java.time.Duration;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;

public class VertxNonceRequestTransmitterFactory {

  private final HttpClient client;
  private final JsonDecoder decoder;
  private final Duration requestTimeout;
  private final DownstreamPathCalculator downstreamPathCalculator;

  public VertxNonceRequestTransmitterFactory(
      final HttpClient client,
      final JsonDecoder decoder,
      final Duration requestTimeout,
      final DownstreamPathCalculator downstreamPathCalculator) {
    this.client = client;
    this.decoder = decoder;
    this.requestTimeout = requestTimeout;
    this.downstreamPathCalculator = downstreamPathCalculator;
  }

  public VertxNonceRequestTransmitter create(final MultiMap headers) {
    return new VertxNonceRequestTransmitter(
        headers, client, decoder, requestTimeout, downstreamPathCalculator);
  }
}

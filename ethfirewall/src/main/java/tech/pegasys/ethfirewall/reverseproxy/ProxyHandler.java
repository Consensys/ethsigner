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
package tech.pegasys.ethfirewall.reverseproxy;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs the JSON-RPC proxy behaviour, forwarding on a sendRequest and writing back the response
 * and dealing with Orion.
 *
 * <p>A terminal handler, one that ends the event, preventing any other handler from executing.
 */
public class ProxyHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(ProxyHandler.class);

  /** The standard Ethereum node proxying behaviour. */
  private final ReverseProxy reverseProxy;

  public ProxyHandler(final ReverseProxy reverseProxy) {
    this.reverseProxy = reverseProxy;
  }

  @Override
  public void handle(final RoutingContext context) {
    logStandardRequest();
    reverseProxy.proxy(context);
  }

  private void logStandardRequest() {
    LOG.debug("Handling standard Ethereum sendRequest");
  }
}

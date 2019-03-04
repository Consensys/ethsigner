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
package tech.pegasys.ethfirewall;

import tech.pegasys.ethfirewall.config.EthFirewallConfig;
import tech.pegasys.ethfirewall.jsonrpc.JsonRpcHttpService;
import tech.pegasys.ethfirewall.jsonrpc.ReverseProxyHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

  public void start(final Vertx vertx) {
    final EthFirewallConfig config = new EthFirewallConfig();

    final ReverseProxyHandler reverseProxyHandler = createReverseProxyHandler(vertx, config);
    final JsonRpcHttpService jsonRpcHttpService =
        new JsonRpcHttpService(config, (ignore) -> reverseProxyHandler);
    vertx.deployVerticle(jsonRpcHttpService, this::handleDeployResult);
  }

  private ReverseProxyHandler createReverseProxyHandler(
      final Vertx vertx, final EthFirewallConfig config) {
    final WebClientOptions clientOptions =
        new WebClientOptions()
            .setDefaultPort(config.getEthPort())
            .setDefaultHost(config.getEthHost());
    return new ReverseProxyHandler(vertx.createHttpClient(clientOptions));
  }

  private void handleDeployResult(final AsyncResult<?> result) {
    if (result.succeeded()) {
      LOG.info("Deployment id is: {}", result.result());
    } else {
      LOG.warn("Deployment failed! ", result.cause());
    }
  }
}

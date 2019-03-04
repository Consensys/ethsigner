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
import tech.pegasys.ethfirewall.reverseproxy.JsonRpcReverseProxy;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
  private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

  public void start(final Vertx vertx) {
    final EthFirewallConfig ethFirewallConfig = new EthFirewallConfig();
    final JsonRpcReverseProxy jsonRpcReverseProxy = new JsonRpcReverseProxy(ethFirewallConfig);
    vertx.deployVerticle(jsonRpcReverseProxy, this::handleDeployResult);
  }

  private void handleDeployResult(final AsyncResult<?> result) {
    if (result.succeeded()) {
      LOG.info("Deployment id is: {}", result.result());
    } else {
      LOG.warn("Deployment failed! ", result.cause());
    }
  }
}

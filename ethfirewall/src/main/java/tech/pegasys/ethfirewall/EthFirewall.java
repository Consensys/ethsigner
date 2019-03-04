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

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EthFirewall {
  private static final Logger LOG = LoggerFactory.getLogger(EthFirewall.class);

  public static void main(final String... args) {
    LOG.info("Running EthFirewall.");

    try {
      final Runner runner = new Runner();
      runner.start(Vertx.vertx());
    } catch (Throwable t) {
      LOG.error("Unexpected exception starting EthFirewall", t);
      System.exit(1);
    }
  }
}

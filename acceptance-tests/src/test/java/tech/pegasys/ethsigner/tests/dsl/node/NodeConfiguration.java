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
package tech.pegasys.ethsigner.tests.dsl.node;

import java.time.Duration;

public class NodeConfiguration {

  private static final String HTTP_URL_FORMAT = "http://%s:%s";

  // TODO make these actual config options
  private static final String LOCALHOST = "127.0.0.1";
  private static final int PORT = 8545;
  private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

  public String hostname() {
    return LOCALHOST;
  }

  public int port() {
    return PORT;
  }

  public Duration pollingInterval() {
    return POLLING_INTERVAL;
  }

  public String downstreamUrl() {
    return String.format(HTTP_URL_FORMAT, hostname(), port());
  }
}

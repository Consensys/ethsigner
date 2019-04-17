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
package tech.pegasys.ethsigner.tests.dsl.signer;

import java.time.Duration;

public class SignerConfiguration {

  private static final String HTTP_URL_FORMAT = "http://%s:%s";
  private static final int TCP_PORT = 8845;
  private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

  /** ChainId defined in the Pantheon dev mode genesis. */
  private static final String CHAIN_ID = "2018";

  private final String hostname;

  public SignerConfiguration(final String hostname) {
    this.hostname = hostname;
  }

  public String hostname() {
    return hostname;
  }

  public int tcpPort() {
    return TCP_PORT;
  }

  public Duration pollingInterval() {
    return POLLING_INTERVAL;
  }

  public String url() {
    return String.format(HTTP_URL_FORMAT, hostname(), tcpPort());
  }

  public String chainId() {
    return CHAIN_ID;
  }
}

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
package tech.pegasys.ethfirewall.config;

public class EthFirewallConfig {
  private String ethHost = "localhost";
  private int ethPort = 8545;
  private int port = 8889;
  private String host = "localhost";

  public String getEthHost() {
    return ethHost;
  }

  public void setEthHost(final String ethHost) {
    this.ethHost = ethHost;
  }

  public int getEthPort() {
    return ethPort;
  }

  public void setEthPort(final int ethPort) {
    this.ethPort = ethPort;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }
}

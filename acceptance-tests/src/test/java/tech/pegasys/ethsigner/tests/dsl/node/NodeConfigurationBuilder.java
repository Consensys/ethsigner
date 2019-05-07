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

import java.net.URI;
import java.util.Optional;

import com.github.dockerjava.core.DefaultDockerClientConfig;

public class NodeConfigurationBuilder {

  private static final String LOCALHOST = "127.0.0.1";
  private static final String DEFAULT_GENESIS_FILE = "eth_hash_2018.json";

  private final DefaultDockerClientConfig config;
  private String genesis;
  private String cors;

  public NodeConfigurationBuilder() {
    this.config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    this.genesis = DEFAULT_GENESIS_FILE;
  }

  private Optional<String> dockerHost(final DefaultDockerClientConfig config) {
    return Optional.of(config.getDockerHost()).map(URI::getHost);
  }

  public NodeConfigurationBuilder withGenesis(final String genesisFile) {
    this.genesis = genesisFile;
    return this;
  }

  public NodeConfiguration build() {
    final String hostname = dockerHost(config).orElse(LOCALHOST);

    return new NodeConfiguration(genesis, hostname, cors);
  }

  public NodeConfigurationBuilder cors(final String cors) {
    this.cors = cors;
    return this;
  }
}

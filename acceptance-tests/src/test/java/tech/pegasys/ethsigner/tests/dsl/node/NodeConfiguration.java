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
import java.util.Optional;

public class NodeConfiguration {

  private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

  private final String hostname;
  private final String genesisFilePath;
  private final Optional<String> cors;
  private final boolean privacyEnabled;
  private final Optional<String> privacyPublicKeyPath;

  public NodeConfiguration(
      final String genesisFilePath,
      final String hostname,
      final String cors,
      final boolean privacyEnabled,
      final String privacyPublicKeyPath) {
    this.hostname = hostname;
    this.genesisFilePath = genesisFilePath;
    this.cors = Optional.ofNullable(cors);
    this.privacyEnabled = privacyEnabled;
    this.privacyPublicKeyPath = Optional.ofNullable(privacyPublicKeyPath);
  }

  public String getHostname() {
    return hostname;
  }

  public Duration getPollingInterval() {
    return POLLING_INTERVAL;
  }

  public String getGenesisFilePath() {
    return genesisFilePath;
  }

  public Optional<String> getCors() {
    return cors;
  }

  public boolean isPrivacyEnabled() {
    return privacyEnabled;
  }

  public Optional<String> getPrivacyPublicKeyPath() {
    return privacyPublicKeyPath;
  }
}

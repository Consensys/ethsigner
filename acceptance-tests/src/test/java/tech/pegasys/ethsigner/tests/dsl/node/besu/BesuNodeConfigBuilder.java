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
package tech.pegasys.ethsigner.tests.dsl.node.besu;

import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class BesuNodeConfigBuilder {
  private Path dataPath;
  private String name;
  private List<String> additionalCommandLineArgs = emptyList();
  private List<String> envVarsToRemove = emptyList();
  private String genesisFile;
  private String cors;

  private BesuNodeConfigBuilder() {}

  public static BesuNodeConfigBuilder aBesuNodeConfig() {
    return new BesuNodeConfigBuilder();
  }

  public BesuNodeConfigBuilder withDataPath(Path dataPath) {
    this.dataPath = dataPath;
    return this;
  }

  public BesuNodeConfigBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public BesuNodeConfigBuilder withAdditionalCommandLineArgs(
      List<String> additionalCommandLineArgs) {
    this.additionalCommandLineArgs = additionalCommandLineArgs;
    return this;
  }

  public BesuNodeConfigBuilder withEnvVarsToRemove(List<String> envVarsToRemove) {
    this.envVarsToRemove = envVarsToRemove;
    return this;
  }

  public BesuNodeConfigBuilder withGenesisFile(String genesisFile) {
    this.genesisFile = genesisFile;
    return this;
  }

  public BesuNodeConfigBuilder withCors(String cors) {
    this.cors = cors;
    return this;
  }

  public BesuNodeConfig build() {
    if (name == null) {
      throw new IllegalArgumentException("name is required");
    }

    return new BesuNodeConfig(
        name,
        Optional.ofNullable(dataPath),
        Optional.ofNullable(genesisFile),
        additionalCommandLineArgs,
        envVarsToRemove,
        Optional.ofNullable(cors));
  }
}
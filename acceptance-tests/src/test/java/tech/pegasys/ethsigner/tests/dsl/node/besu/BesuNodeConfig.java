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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BesuNodeConfig {

  private final Optional<Path> dataPath;
  private final String name;
  private final List<String> additionalCommandLineArgs;
  private final List<String> envVarsToRemove;
  private final Optional<String> genesisFile;
  private final Optional<String> cors;

  public BesuNodeConfig(
      final String name,
      final Optional<Path> dataPath,
      final Optional<String> genesisFile,
      final List<String> additionalCommandLineArgs,
      final List<String> envVarsToRemove,
      final Optional<String> cors) {
    this.dataPath = dataPath;
    this.genesisFile = genesisFile;
    this.name = name;
    this.additionalCommandLineArgs = additionalCommandLineArgs;
    this.envVarsToRemove = envVarsToRemove;
    this.cors = cors;
  }

  public Optional<Path> getDataPath() {
    return dataPath;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getGenesisFile() {
    return genesisFile;
  }

  public List<String> getAdditionalCommandLineArgs() {
    return additionalCommandLineArgs;
  }

  public List<String> getEnvironmentVariablesToRemove() {
    return envVarsToRemove;
  }

  public Optional<String> getCors() {
    return cors;
  }
}

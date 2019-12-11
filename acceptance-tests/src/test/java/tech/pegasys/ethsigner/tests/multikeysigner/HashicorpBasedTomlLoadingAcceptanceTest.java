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
package tech.pegasys.ethsigner.tests.multikeysigner;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.utils.HashicorpHelpers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HashicorpBasedTomlLoadingAcceptanceTest extends MultiKeyAcceptanceTestBase {

  static final String FILENAME = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";
  static final String HASHICORP_ETHEREUM_ADDRESS = "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73";

  @BeforeAll
  static void setUpBase() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(() -> HashicorpHelpers.tearDownHashicorpVault(hashicorpVaultDocker)));

    hashicorpVaultDocker = HashicorpHelpers.setUpHashicorpVault(new DockerClientFactory().create());
  }

  @Test
  void hashicorpSignerIsCreatedAndExpectedAddressIsReported() {
    createHashicorpTomlFileAt(
        FILENAME + ".toml",
        HashicorpHelpers.keyPath,
        HashicorpHelpers.vaultAuthFile,
        hashicorpVaultDocker);
    setup();
    assertThat(ethSigner.accounts().list()).containsOnly(HASHICORP_ETHEREUM_ADDRESS);
  }

  @Test
  void incorrectlyNamedHashicorpConfigFileIsNotLoaded() {
    createHashicorpTomlFileAt(
        "ffffffffffffffffffffffffffffffffffffffff.toml",
        HashicorpHelpers.keyPath,
        HashicorpHelpers.vaultAuthFile,
        hashicorpVaultDocker);
    setup();
    assertThat(ethSigner.accounts().list()).isEmpty();
  }
}

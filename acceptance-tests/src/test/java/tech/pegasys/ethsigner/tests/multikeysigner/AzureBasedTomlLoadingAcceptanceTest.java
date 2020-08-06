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

import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AzureBasedTomlLoadingAcceptanceTest extends MultiKeyAcceptanceTestBase {

  static final String clientId = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
  static final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");
  static final String tenantId = System.getenv("ETHSIGNER_AZURE_TENANT_ID");
  static final String FILENAME = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";
  static final String AZURE_ETHEREUM_ADDRESS = "0x" + FILENAME;

  @BeforeAll
  public static void checkAzureCredentials() {
    Assumptions.assumeTrue(
        clientId != null && clientSecret != null && tenantId != null,
        "Ensure Azure client id, client secret and tenant_id env variables are set");
  }

  @Test
  void azureSignersAreCreatedAndExpectedAddressIsReported(@TempDir Path tomlDirectory) {
    createAzureTomlFileAt(
        tomlDirectory.resolve("arbitrary_prefix" + FILENAME + ".toml"),
        clientId,
        clientSecret,
        tenantId);

    setup(tomlDirectory);

    assertThat(ethSigner.accounts().list()).containsOnly(AZURE_ETHEREUM_ADDRESS);
  }

  @Test
  void incorrectlyNamedAzureFileIsNotLoaded(@TempDir Path tomlDirectory) {
    createAzureTomlFileAt(
        tomlDirectory.resolve("ffffffffffffffffffffffffffffffffffffffff.toml"),
        clientId,
        clientSecret,
        tenantId);

    setup(tomlDirectory);

    assertThat(ethSigner.accounts().list()).isEmpty();
  }
}

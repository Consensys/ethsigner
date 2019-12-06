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
package tech.pegasys.ethsigner.tests.multiplatformsigner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AzureBasedTomlLoadingAcceptanceTest extends MultiPlatformAcceptanceTestBase {

  static final String clientId = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
  static final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");
  static final String AZURE_ETHEREUM_ADDRESS = "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73";

  @BeforeAll
  static void preChecks() {
    Assumptions.assumeTrue(
        clientId != null && clientSecret != null,
        "Ensure Azure client id and client secret env variables are set");
  }

  @Test
  void azureSignersAreCreatedAndExpectedAddressIsReported() {
    createAzureTomlFileAt(AZURE_ETHEREUM_ADDRESS + ".toml", clientId, clientSecret);

    setup();

    assertThat(ethSigner.accounts().list()).containsOnly(AZURE_ETHEREUM_ADDRESS);
  }

  @Test
  void incorrectlyNamedAzureFileIsNotLoaded() {
    createAzureTomlFileAt("ffffffffffffffffffffffffffffffffffff‰ffff.toml", clientId, clientSecret);

    setup();

    assertThat(ethSigner.accounts().list()).isEmpty();
  }
}

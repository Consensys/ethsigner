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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.multikeysigner.AzureBasedTomlLoadingAcceptanceTest.AZURE_ETHEREUM_ADDRESS;
import static tech.pegasys.ethsigner.tests.multikeysigner.FileBasedTomlLoadingAcceptanceTest.FILE_ETHEREUM_ADDRESS;
import static tech.pegasys.ethsigner.tests.multikeysigner.HashicorpBasedTomlLoadingAcceptanceTest.HASHICORP_ETHEREUM_ADDRESS;

import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.utils.HashicorpNode;
import tech.pegasys.ethsigner.tests.hashicorpvault.HashicorpVaultDocker;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiKeySigningAcceptanceTest extends MultiKeyAcceptanceTestBase {

  @TempDir static Path tempDir;
  private static String authFilename;
  private static HashicorpVaultDocker hashicorpVaultDocker;
  private static final HashicorpNode hashicorpNode = new HashicorpNode();

  @BeforeAll
  static void preSetup() throws IOException {
    preChecks();

    hashicorpVaultDocker = hashicorpNode.start(new DockerClientFactory().create());

    final Path authFilePath = tempDir.resolve("hashicorpAuthFile");
    Files.write(authFilePath, hashicorpVaultDocker.getVaultToken().getBytes(UTF_8));
    authFilename = authFilePath.toAbsolutePath().toString();
  }

  static void preChecks() {
    Assumptions.assumeTrue(
        AzureBasedTomlLoadingAcceptanceTest.clientId != null
            && AzureBasedTomlLoadingAcceptanceTest.clientSecret != null,
        "Ensure Azure client id and client secret env variables are set");
  }

  @Test
  void multipleSignersAreCreatedAndExpectedAddressAreReported() throws URISyntaxException {

    createAzureTomlFileAt(
        AZURE_ETHEREUM_ADDRESS + ".toml",
        AzureBasedTomlLoadingAcceptanceTest.clientId,
        AzureBasedTomlLoadingAcceptanceTest.clientSecret,
        tempDir);
    createFileBasedTomlFileAt(
        "a01f618424b0113a9cebdc6cb66ca5b48e9120c5.toml",
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath(),
        tempDir);

    createHashicorpTomlFileAt(
        HASHICORP_ETHEREUM_ADDRESS + ".toml",
        HashicorpVaultDocker.absKeyPath,
        authFilename,
        hashicorpVaultDocker,
        tempDir);

    setup(tempDir);

    assertThat(ethSigner.accounts().list())
        .containsOnly(AZURE_ETHEREUM_ADDRESS, FILE_ETHEREUM_ADDRESS, HASHICORP_ETHEREUM_ADDRESS);
  }

  @AfterAll
  static void tearDown() {
    hashicorpNode.shutdown(hashicorpVaultDocker);
  }
}

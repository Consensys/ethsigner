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
package tech.pegasys.ethsigner.tests.multikeysigner.transactionsigning;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.hashicorp.HashicorpNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MultiKeyHashicorpTransactionSignerAcceptanceTest
    extends MultiKeyTransactionSigningAcceptanceTestBase {

  static final String FILENAME = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";

  private static HashicorpNode hashicorpNode;

  @BeforeAll
  static void preSetup() {
    hashicorpNode =
        HashicorpNode.createAndStartHashicorp(new DockerClientFactory().create(), false);
  }

  @Test
  void hashicorpLoadedFromMultiKeyCanSignValueTransferTransaction(@TempDir Path tomlDirectory)
      throws IOException {

    final Path authFilePath = tomlDirectory.resolve("hashicorpAuthFile");
    Files.write(authFilePath, hashicorpNode.getVaultToken().getBytes(UTF_8));
    final String authFilename = authFilePath.toAbsolutePath().toString();

    createHashicorpTomlFileAt(
        tomlDirectory.resolve(FILENAME + ".toml"), authFilename, hashicorpNode);

    setup(tomlDirectory);
    performTransaction();
  }

  @AfterAll
  static void tearDown() {
    if (hashicorpNode != null) {
      hashicorpNode.shutdown();
      hashicorpNode = null;
    }
  }
}

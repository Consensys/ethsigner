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
package tech.pegasys.ethsigner.tests.dsl.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.TransactionSignerParamsSupplier;
import tech.pegasys.ethsigner.tests.hashicorpvault.HashicorpVaultDocker;

import java.io.File;

import com.github.dockerjava.api.DockerClient;

public class HashicorpHelpers {

  public static String keyPath = "/secret/data/ethsignerSigningKey";

  public static HashicorpVaultDocker setUpHashicorpVault(final DockerClient docker) {
    HashicorpVaultDocker hashicorpVaultDocker = new HashicorpVaultDocker(docker);
    hashicorpVaultDocker.start();
    hashicorpVaultDocker.awaitStartupCompletion();
    hashicorpVaultDocker.createTestData();
    return hashicorpVaultDocker;
  }

  public static void tearDownHashicorpVault(final HashicorpVaultDocker hashicorpVaultDocker) {
    if (hashicorpVaultDocker != null) {
      hashicorpVaultDocker.shutdown();
    }
  }
}

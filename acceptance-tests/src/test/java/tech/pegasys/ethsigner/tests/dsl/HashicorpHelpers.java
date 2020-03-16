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
package tech.pegasys.ethsigner.tests.dsl;

import static java.util.Collections.singletonMap;

import tech.pegasys.ethsigner.tests.dsl.node.HashicorpSigningParams;
import tech.pegasys.signers.hashicorp.dsl.HashicorpNode;

import com.github.dockerjava.api.DockerClient;

public class HashicorpHelpers {

  public static final String secretPath = "acceptanceTestSecretPath";
  public static final String secretName = "value"; // this is the required default usedin EthSigner
  private static final String PRIVATE_KEY_HEX_STRING =
      "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";

  public static HashicorpSigningParams createLoadedHashicorpVault(
      final DockerClient docker, boolean useTls) {
    final HashicorpNode hashicorpNode = HashicorpNode.createAndStartHashicorp(docker, useTls);
    hashicorpNode.addSecretsToVault(singletonMap(secretName, PRIVATE_KEY_HEX_STRING), secretPath);

    return new HashicorpSigningParams(hashicorpNode, secretPath, secretName);
  }
}

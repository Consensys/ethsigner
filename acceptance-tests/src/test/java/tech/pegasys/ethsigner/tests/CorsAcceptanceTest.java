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
package tech.pegasys.ethsigner.tests;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.PantheonNode;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.io.IOException;

import com.github.dockerjava.api.DockerClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CorsAcceptanceTest {

  private static final DockerClient DOCKER = new DockerClientFactory().create();
  private Node ethNode;
  private Signer ethSigner;

  private final String AUTHORISED_DOMAIN = "authorised.com";
  private final String UNAUTHORISED_DOMAIN = "UN" + AUTHORISED_DOMAIN;

  @Before
  public void setUp() {
    final NodeConfiguration nodeConfig =
        new NodeConfigurationBuilder().cors(AUTHORISED_DOMAIN).build();
    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();

    ethSigner = new Signer(signerConfig, nodeConfig);
    ethNode = new PantheonNode(DOCKER, nodeConfig);

    ethNode.start();
    ethSigner.start();

    ethNode.awaitStartupCompletion();
    ethSigner.awaitStartupCompletion();
  }

  @After
  public void tearDown() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }

  @Test
  public void forbiddenResponseReceivedWhenHeadersDoNotMatchCorsOfNode() throws IOException {
    final SignerResponse<JsonRpcErrorResponse> response =
        ethSigner
            .rawRequest()
            .exceptionalRequest("eth_blockNumber", singletonMap("origin", UNAUTHORISED_DOMAIN));

    assertThat(response.status()).isEqualTo(HttpResponseStatus.FORBIDDEN);
    assertThat(response.jsonRpc()).isNull();
  }
}

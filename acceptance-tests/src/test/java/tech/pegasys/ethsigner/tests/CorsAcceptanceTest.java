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

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfig;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfigBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeFactory;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CorsAcceptanceTest {

  private Node ethNode;
  private Signer ethSigner;

  private final String AUTHORISED_DOMAIN = "authorised.com";
  private final String UNAUTHORISED_DOMAIN = "UN" + AUTHORISED_DOMAIN;

  @BeforeEach
  public void setUp() {
    final BesuNodeConfig besuNodeConfig =
        BesuNodeConfigBuilder.aBesuNodeConfig().withCors(AUTHORISED_DOMAIN).build();
    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();

    ethNode = BesuNodeFactory.create(besuNodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    ethSigner = new Signer(signerConfig, "127.0.0.1", ethNode.ports());
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  @AfterEach
  public void tearDown() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }

  @Test
  public void forbiddenResponseReceivedWhenHeadersDoNotMatchCorsOfNode() {
    final SignerResponse<JsonRpcErrorResponse> response =
        ethSigner
            .rawJsonRpcRequests()
            .exceptionalRequest(
                "eth_blockNumber",
                singletonMap(HttpHeaderNames.ORIGIN.toString(), UNAUTHORISED_DOMAIN));

    assertThat(response.status()).isEqualTo(HttpResponseStatus.FORBIDDEN);
    assertThat(response.jsonRpc()).isNull();
  }
}

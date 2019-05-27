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
package tech.pegasys.ethsigner.tests.signing;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.ENCLAVE_ERROR;
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_LIMIT;
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.PrivateTransaction.RESTRICTED;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.PrivateTransaction;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.PantheonNode;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;
import tech.pegasys.ethsigner.tests.signing.contract.generated.SimpleStorage;

import java.math.BigInteger;

import com.github.dockerjava.api.DockerClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PrivateTransactionAcceptanceTest {

  private static final DockerClient DOCKER = new DockerClientFactory().create();

  // TODO where should these be defined?
  private static final String ORION_PUBLIC_KEY1 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String ORION_PUBLIC_KEY2 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  private Node ethNode;
  private Signer ethSigner;

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  @Before
  public void setUp() {
    Runtime.getRuntime().addShutdownHook(new Thread((this::tearDown)));

    final NodeConfiguration nodeConfig =
        new NodeConfigurationBuilder()
            .withPrivacyEnabled()
            .withPrivacyPublicKey("orion_key1.pub")
            .build();
    ethNode = new PantheonNode(DOCKER, nodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    final SignerConfiguration signerConfig = new SignerConfigurationBuilder().build();
    ethSigner = new Signer(signerConfig, nodeConfig, ethNode.ports());
    ethSigner.start();
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
  public void submittingContractWithoutOrionFailsWithEnclaveError() {
    final PrivateTransaction contract =
        PrivateTransaction.createContractTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            BigInteger.ZERO,
            SimpleStorage.BINARY,
            ORION_PUBLIC_KEY1,
            singletonList(ORION_PUBLIC_KEY2),
            RESTRICTED);

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner.contracts().submitExceptionalPrivateTransaction(contract);
    assertThat(signerResponse.status()).isEqualTo(BAD_REQUEST);
    assertThat(signerResponse.jsonRpc().getError()).isEqualTo(ENCLAVE_ERROR);
  }
}

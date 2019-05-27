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
import static java.nio.charset.StandardCharsets.UTF_8;
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

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;

import com.github.dockerjava.api.DockerClient;
import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PrivateTransactionAcceptanceTest {

  private static final DockerClient DOCKER = new DockerClientFactory().create();

  private static final String ENCLAVE_PUBLIC_KEY1 = "enclave_key1.pub";
  private static final String ENCLAVE_PUBLIC_KEY2 = "enclave_key2.pub";

  private Node ethNode;
  private Signer ethSigner;

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  private String enclavePublicKey1() {
    return readResource(ENCLAVE_PUBLIC_KEY1);
  }

  private String enclavePublicKey2() {
    return readResource(ENCLAVE_PUBLIC_KEY2);
  }

  @SuppressWarnings("UnstableApiUsage")
  private String readResource(final String resourceName) {
    final URL resource = Resources.getResource(resourceName);
    try {
      return new String(Resources.toString(resource, UTF_8).getBytes(UTF_8), UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load resource " + resourceName);
    }
  }

  @Before
  public void setUp() {
    Runtime.getRuntime().addShutdownHook(new Thread((this::tearDown)));

    final NodeConfiguration nodeConfig =
        new NodeConfigurationBuilder()
            .withPrivacyEnabled()
            .withPrivacyPublicKey(ENCLAVE_PUBLIC_KEY2)
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
  public void deployContract() {
    final PrivateTransaction contract =
        PrivateTransaction.createContractTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            BigInteger.ZERO,
            SimpleStorage.BINARY,
            enclavePublicKey1(),
            singletonList(enclavePublicKey2()),
            RESTRICTED);

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner.privateContracts().submitExceptional(contract);
    // We expect this to fail with enclave error as we don't have orion running. If rlp decode fails
    // then we would get a different error
    assertThat(signerResponse.status()).isEqualTo(BAD_REQUEST);
    assertThat(signerResponse.jsonRpc().getError()).isEqualTo(ENCLAVE_ERROR);
  }
}

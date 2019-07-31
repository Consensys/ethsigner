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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.tests.dsl.http.HttpResponse;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;

import java.net.SocketTimeoutException;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UpCheckAcceptanceTest {

  private static final String UP_CHECK_PATH = "/upcheck";
  private static final String UP_CHECK_MESSAGE = "I'm up!";
  private static final String TIMEOUT_MESSAGE = "timeout";

  private static Signer ethSigner;

  @BeforeAll
  public static void setUpBase() {
    Runtime.getRuntime()
        .addShutdownHook(new Thread(DataPathFeatureFlagAcceptanceTest::tearDownBase));

    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();
    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withHttpRpcPort(7009).withWebSocketPort(7010).build();

    ethSigner = new Signer(signerConfig, nodeConfig, new NodePorts(0, 0));
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  @Test
  public void getRequestWithWrongPathMustTimeout() {
    final SocketTimeoutException reply =
        ethSigner.httpRequests().getExceptingTimeout(UP_CHECK_PATH + "Noise");

    assertThat(reply).isNotNull();
  }

  @Test
  public void getRequestMustRespond() {
    final HttpResponse reply = ethSigner.httpRequests().get(UP_CHECK_PATH);

    assertThat(reply.status()).isEqualTo(HttpResponseStatus.OK);
    assertThat(reply.body()).isEqualTo(UP_CHECK_MESSAGE);
  }

  @Test
  public void postRequestMustTimeout() {
    final SocketTimeoutException reply =
        ethSigner.httpRequests().postExceptingTimeout(UP_CHECK_PATH);

    assertThat(reply).isNotNull();
  }

  @Test
  public void putRequestMustTimeout() {
    final SocketTimeoutException reply =
        ethSigner.httpRequests().putExceptingTimeout(UP_CHECK_PATH);

    assertThat(reply).isNotNull();
  }

  @Test
  public void deleteRequestMustTimeout() {
    final SocketTimeoutException reply =
        ethSigner.httpRequests().deleteExceptingTimeout(UP_CHECK_PATH);

    assertThat(reply).isNotNull();
  }
}

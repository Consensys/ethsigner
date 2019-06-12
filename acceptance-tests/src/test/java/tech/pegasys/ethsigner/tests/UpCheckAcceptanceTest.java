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

import java.net.SocketTimeoutException;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

public class UpCheckAcceptanceTest extends AcceptanceTestBase {

  private static final String UP_CHECK_PATH = "/upcheck";
  private static final String UP_CHECK_MESSAGE = "I'm up!";
  private static final String TIMEOUT_MESSAGE = "timeout";

  @Test
  public void getRequestWithWrongPathMustRespond() {
    final SocketTimeoutException reply =
        ethSigner().httpRequests().getExceptingTimeout(UP_CHECK_PATH + "Noise");

    assertThat(reply).isNotNull();
    assertThat(reply.getMessage()).isEqualTo(TIMEOUT_MESSAGE);
  }

  @Test
  public void getRequestMustRespond() {
    final HttpResponse reply = ethSigner().httpRequests().get(UP_CHECK_PATH);

    assertThat(reply.status()).isEqualTo(HttpResponseStatus.OK);
    assertThat(reply.body()).isEqualTo(UP_CHECK_MESSAGE);
  }

  @Test
  public void postRequestMustTimeout() {
    final SocketTimeoutException reply =
        ethSigner().httpRequests().postExceptingTimeout(UP_CHECK_PATH);

    assertThat(reply).isNotNull();
    assertThat(reply.getMessage()).isEqualTo(TIMEOUT_MESSAGE);
  }

  @Test
  public void putRequestMustTimeout() {
    final SocketTimeoutException reply =
        ethSigner().httpRequests().putExceptingTimeout(UP_CHECK_PATH);

    assertThat(reply).isNotNull();
    assertThat(reply.getMessage()).isEqualTo(TIMEOUT_MESSAGE);
  }

  @Test
  public void deleteRequestMustTimeout() {
    final SocketTimeoutException reply =
        ethSigner().httpRequests().deleteExceptingTimeout(UP_CHECK_PATH);

    assertThat(reply).isNotNull();
    assertThat(reply.getMessage()).isEqualTo(TIMEOUT_MESSAGE);
  }
}

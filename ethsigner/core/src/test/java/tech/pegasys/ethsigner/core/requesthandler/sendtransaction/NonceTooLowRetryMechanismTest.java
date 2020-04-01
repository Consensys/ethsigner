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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;

import java.math.BigInteger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NonceTooLowRetryMechanismTest {

  private final NonceProvider nonceProvider = mock(NonceProvider.class);
  private final HttpClientResponse httpResponse = mock(HttpClientResponse.class);

  private final RetryMechanism retryMechanism = new NonceTooLowRetryMechanism(2);

  @BeforeEach
  public void setup() {
    when(nonceProvider.getNonce()).thenReturn(BigInteger.ONE);
  }

  @Test
  public void retryIsNotRequiredIfErrorIsNotNonceTooLow() {
    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.BAD_REQUEST.code());

    final JsonRpcErrorResponse errorResponse =
        new JsonRpcErrorResponse(JsonRpcError.INVALID_PARAMS);

    assertThat(
            retryMechanism.responseRequiresRetry(httpResponse, Json.encodeToBuffer(errorResponse)))
        .isFalse();
  }

  @Test
  public void retryIsNotRequiredForUnknownErrorType() {
    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.BAD_REQUEST.code());

    final JsonObject errorResponse = new JsonObject();
    final JsonObject error = new JsonObject();
    error.put("code", -9000);
    error.put("message", "Unknown error");
    errorResponse.put("jsonrpc", "2.0");
    errorResponse.put("id", 1);
    errorResponse.put("error", new JsonObject());

    assertThat(
            retryMechanism.responseRequiresRetry(httpResponse, Json.encodeToBuffer(errorResponse)))
        .isFalse();
  }

  @Test
  public void testRetryReportsFalseOnceMatchingMaxValue() {
    assertThat(retryMechanism.retriesAvailable()).isTrue();
    retryMechanism.incrementRetries(); // retried once
    assertThat(retryMechanism.retriesAvailable()).isTrue();
    retryMechanism.incrementRetries(); // retried twice
    assertThat(retryMechanism.retriesAvailable()).isFalse();
    retryMechanism.incrementRetries();
    assertThat(retryMechanism.retriesAvailable()).isFalse();
  }
}

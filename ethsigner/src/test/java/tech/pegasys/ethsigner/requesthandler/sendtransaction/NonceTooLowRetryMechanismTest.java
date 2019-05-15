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
package tech.pegasys.ethsigner.requesthandler.sendtransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.jsonrpc.SendTransactionJsonParameters;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import java.math.BigInteger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import org.junit.Before;
import org.junit.Test;

public class NonceTooLowRetryMechanismTest {

  private final NonceProvider nonceProvider = mock(NonceProvider.class);
  private final HttpClientResponse httpResponse = mock(HttpClientResponse.class);
  private SendTransactionContext context;

  private final RetryMechanism<SendTransactionContext> retryMechanism =
      new NonceTooLowRetryMechanism(nonceProvider);

  @Before
  public void setup() {
    when(nonceProvider.getNonce()).thenReturn(BigInteger.ONE);
    context =
        new SendTransactionContext(
            null,
            new JsonRpcRequestId(1),
            new EthTransaction(new SendTransactionJsonParameters("0x1234")));
  }

  @Test
  public void retryIsNotRequiredIfErrorIsNotNonceTooLow() {
    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.BAD_REQUEST.code());

    final JsonRpcErrorResponse errorResponse =
        new JsonRpcErrorResponse(JsonRpcError.INVALID_PARAMS);

    assertThat(retryMechanism.mustRetry(httpResponse, Json.encodeToBuffer(errorResponse)))
        .isFalse();
  }

  @Test
  public void retriesAreNotAttemptedAfterFiveTimes() {
    when(httpResponse.statusCode()).thenReturn(HttpResponseStatus.BAD_REQUEST.code());

    final JsonRpcErrorResponse errorResponse = new JsonRpcErrorResponse(JsonRpcError.NONCE_TOO_LOW);

    for (int i = 0; i < 5; i++) {
      assertThat(retryMechanism.mustRetry(httpResponse, Json.encodeToBuffer(errorResponse)))
          .isTrue();
      retryMechanism.retry(context, () -> {});
    }
    assertThat(retryMechanism.mustRetry(httpResponse, Json.encodeToBuffer(errorResponse)))
        .isFalse();
  }
}

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

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import java.io.IOException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonceTooLowRetryMechanism implements RetryMechanism {

  private static final Logger LOG = LoggerFactory.getLogger(NonceTooLowRetryMechanism.class);

  final RawTransactionBuilder transactionBuilder;
  final NonceProvider nonceProvider;

  public NonceTooLowRetryMechanism(
      RawTransactionBuilder transactionBuilder, NonceProvider nonceProvider) {
    this.transactionBuilder = transactionBuilder;
    this.nonceProvider = nonceProvider;
  }

  @Override
  public boolean shouldRetry(final HttpClientResponse response, final Buffer body)
      throws IOException {
    if ((response.statusCode() == HttpResponseStatus.BAD_REQUEST.code())) {
      final JsonRpcErrorResponse errorResponse = specialiseResponse(body);
      if (errorResponse.getError().equals(JsonRpcError.NONCE_TOO_LOW)) {
        LOG.info("Nonce too low, resetting nonce and resending {}.", errorResponse.getId());
        transactionBuilder.updateNonce(nonceProvider.getNonce());
        return true;
      }
    }
    return false;
  }

  private JsonRpcErrorResponse specialiseResponse(final Buffer body) {
    final JsonObject jsonBody = new JsonObject(body);
    return jsonBody.mapTo(JsonRpcErrorResponse.class);
  }
}

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
package ethsigner.requesthandler.sendtransaction;

import ethsigner.jsonrpc.response.JsonRpcError;
import ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NonceTooLowRetryMechanism implements RetryMechanism<SendTransactionContext> {

  private static final int MAX_RETRIES = 5;

  private static final Logger LOG = LogManager.getLogger();

  private final NonceProvider nonceProvider;

  private int retriesPerformed = 0;

  public NonceTooLowRetryMechanism(final NonceProvider nonceProvider) {
    this.nonceProvider = nonceProvider;
  }

  @Override
  public boolean mustRetry(final HttpClientResponse response, final Buffer body) {
    if ((response.statusCode() == HttpResponseStatus.BAD_REQUEST.code())) {
      final JsonRpcErrorResponse errorResponse = specialiseResponse(body);
      if (errorResponse.getError().equals(JsonRpcError.NONCE_TOO_LOW)) {
        LOG.info("Nonce too low, resend required for {}.", errorResponse.getId());
        return retriesAvailable();
      }
    }
    return false;
  }

  @Override
  public void retry(final SendTransactionContext context, final Runnable sender)
      throws RetryException {
    if (retriesAvailable()) {
      try {
        context.getRawTransactionBuilder().updateNonce(nonceProvider.getNonce());
      } catch (final RuntimeException e) {
        LOG.info("Failed to determine current nonce from web3j provider");
        throw new RetryException();
      }
      retriesPerformed++;
      sender.run();
    } else {
      LOG.error("Attempting to resend when retries are exhausted.");
    }
  }

  private JsonRpcErrorResponse specialiseResponse(final Buffer body) {
    final JsonObject jsonBody = new JsonObject(body);
    return jsonBody.mapTo(JsonRpcErrorResponse.class);
  }

  private boolean retriesAvailable() {
    return retriesPerformed < MAX_RETRIES;
  }
}

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

import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.NONCE_TOO_LOW;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NonceTooLowRetryMechanism extends RetryMechanism {

  private static final Logger LOG = LogManager.getLogger();

  public NonceTooLowRetryMechanism(final int maxRetries) {
    super(maxRetries);
  }

  @Override
  public boolean responseRequiresRetry(final HttpClientResponse response, final Buffer body) {
    if ((response.statusCode() == HttpResponseStatus.BAD_REQUEST.code())) {
      final JsonRpcErrorResponse errorResponse = specialiseResponse(body);
      if (NONCE_TOO_LOW.equals(errorResponse.getError())) {
        LOG.info("Nonce too low, resend required for {}.", errorResponse.getId());
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

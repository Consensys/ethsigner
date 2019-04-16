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
import tech.pegasys.ethsigner.signing.TransactionSigner;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryingTransactionTransmitter extends TransactionPassthrough {

  private static final Logger LOG = LoggerFactory.getLogger(RetryingTransactionTransmitter.class);

  public RetryingTransactionTransmitter(
      final HttpClient ethNodeClient,
      final TransactionInformation transactionInfo,
      final TransactionSigner signer) {
    super(ethNodeClient, transactionInfo, signer);
  }

  @Override
  protected void handleResponseBody(final HttpClientResponse response, final Buffer body) {
    if ((response.statusCode() == HttpResponseStatus.BAD_REQUEST.code()) && isLowNonceError(body)) {
      LOG.info(
          "Nonce too low, resetting nonce and resubmitting {}.", transactionInfo.getReceivedId());
      send();
    } else {
      super.handleResponseBody(response, body);
    }
  }

  private boolean isLowNonceError(final Buffer body) {
    final JsonObject jsonBody = new JsonObject(body);
    if (jsonBody.containsKey("error") && jsonBody.getValue("error") != null) {
      final JsonObject errorContent = jsonBody.getJsonObject("error");
      return errorContent.getInteger("code").equals(JsonRpcError.NONCE_TOO_LOW.getCode());
    }
    return false;
  }
}

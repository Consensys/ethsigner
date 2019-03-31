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
package tech.pegasys.ethfirewall.jsonrpcproxy;

import tech.pegasys.ethfirewall.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethfirewall.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethfirewall.jsonrpc.SendTransactionJsonParameters;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendTransactionBodyProvider implements BodyProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SendTransactionBodyProvider.class);
  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  private final TransactionSigner signer;

  public SendTransactionBodyProvider(final TransactionSigner transactionSigner) {
    this.signer = transactionSigner;
  }

  @Override
  public JsonRpcBody getBody(final JsonRpcRequest request) {

    final SendTransactionJsonParameters params;
    try {
      params = (SendTransactionJsonParameters) request.getParams();
    } catch (final NumberFormatException e) {

      // TODO fix this - Jackson wraps as NFR
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      return errorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);

    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Deserialisation failed for request: {}", request.getParams(), e);
      return errorResponse(request.getId(), JsonRpcError.INVALID_REQUEST);
    }

    final String signedTransactionHexString = signer.signTransaction(params);

    final JsonRpcRequest sendRawTransaction =
        new JsonRpcRequest(JSON_RPC_VERSION, JSON_RPC_METHOD, signedTransactionHexString);
    sendRawTransaction.setId(request.getId());

    try {
      return new JsonRpcBody(Json.encodeToBuffer(sendRawTransaction));
    } catch (final IllegalArgumentException exception) {
      LOG.debug("JSON Serialisation failed for: {}", sendRawTransaction, exception);
      return errorResponse(request.getId(), JsonRpcError.INTERNAL_ERROR);
    }
  }

  private JsonRpcBody errorResponse(final JsonRpcRequestId id, final JsonRpcError error) {
    return new JsonRpcBody(new JsonRpcErrorResponse(id, error));
  }

  private JsonRpcRequestId id(final JsonObject requestJson) {
    return new JsonRpcRequestId(requestJson.getValue("id"));
  }
}

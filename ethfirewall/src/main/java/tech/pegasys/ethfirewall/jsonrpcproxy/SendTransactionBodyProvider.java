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
import tech.pegasys.ethfirewall.jsonrpc.SendTransactionJsonRpcRequest;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendTransactionBodyProvider implements BodyProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SendTransactionBodyProvider.class);
  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  static {
    // Force Jackson to fail when @JsonCreator values are missing
    Json.mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    Json.mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
  }

  private final TransactionSigner signer;

  public SendTransactionBodyProvider(final TransactionSigner transactionSigner) {
    this.signer = transactionSigner;
  }

  @Override
  public JsonRpcBody getBody(RoutingContext context) {
    final SendTransactionJsonRpcRequest request;
    JsonRpcRequestId id = null;
    final JsonObject requestJson;

    try {
      requestJson = context.getBodyAsJson();
    } catch (final IllegalArgumentException exception) {
      LOG.debug("Deserialisation to JSON failed for: {}", context.getBodyAsString(), exception);
      return errorResponse(id);
    }

    try {
      id = id(requestJson);
      request = requestJson.mapTo(SendTransactionJsonRpcRequest.class);
    } catch (final IllegalArgumentException exception) {
      LOG.debug("JSON Deserialisation failed for request: {}", requestJson, exception);
      return errorResponse(id);
    }

    final String signedTransactionHexString = signer.signTransaction(request);

    final JsonRpcRequest sendRawTransaction =
        new JsonRpcRequest(
            JSON_RPC_VERSION, JSON_RPC_METHOD, new Object[] {signedTransactionHexString});
    sendRawTransaction.setId(id);

    try {
      return new JsonRpcBody(Json.encodeToBuffer(sendRawTransaction));
    } catch (final IllegalArgumentException exception) {
      LOG.debug("JSON Serialisation failed for: {}", sendRawTransaction, exception);
      return errorResponse(id);
    }
  }

  private JsonRpcBody errorResponse(final JsonRpcRequestId id) {
    return new JsonRpcBody(new JsonRpcErrorResponse(id, JsonRpcError.INVALID_REQUEST));
  }

  private JsonRpcRequestId id(final JsonObject requestJson) {
    return new JsonRpcRequestId(requestJson.getValue("id"));
  }
}

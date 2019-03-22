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
import tech.pegasys.ethfirewall.jsonrpc.SignTransactionJsonRpcRequest;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TransactionBodyProvider implements BodyProvider {

  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  private final TransactionSigner signer;

  public TransactionBodyProvider(final TransactionSigner transactionSigner) {
    this.signer = transactionSigner;
  }

  @Override
  public Buffer getBody(RoutingContext context) {
    final SignTransactionJsonRpcRequest request;
    JsonRpcRequestId id = null;

    // TODO generics? for the type?
    try {
      final JsonObject requestJson = context.getBodyAsJson();
      id = id(requestJson);
      request = requestJson.mapTo(SignTransactionJsonRpcRequest.class);
    } catch (final IllegalArgumentException exception) {

      // TODO need to abort - don't make the proxy
      // TODO log info & debug
      return errorResponse(id);
    }

    final String signedTransactionHexString = signer.signTransaction(request);

    final JsonRpcRequest sendRawTransaction =
        new JsonRpcRequest(
            JSON_RPC_VERSION, JSON_RPC_METHOD, new Object[] {signedTransactionHexString});
    sendRawTransaction.setId(id);

    // TODO any problems signing - exit & don't proxy
    return Json.encodeToBuffer(sendRawTransaction);
  }

  private Buffer errorResponse(final Object id) {
    return Json.encodeToBuffer(new JsonRpcErrorResponse(id, JsonRpcError.INVALID_REQUEST));
  }

  private JsonRpcRequestId id(final JsonObject requestJson) {
    return new JsonRpcRequestId(requestJson.getValue("id"));
  }
}

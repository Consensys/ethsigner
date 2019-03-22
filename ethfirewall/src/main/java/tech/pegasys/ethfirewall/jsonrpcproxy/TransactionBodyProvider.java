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

import tech.pegasys.ethfirewall.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethfirewall.jsonrpc.SignTransactionJsonRpcRequest;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethfirewall.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TransactionBodyProvider implements BodyProvider {

  private final TransactionSigner signer;

  public TransactionBodyProvider(final TransactionSigner transactionSigner) {
    this.signer = transactionSigner;
  }

  @Override
  public Buffer getBody(RoutingContext context) {
    final SignTransactionJsonRpcRequest request;
    Object id = null;

    // TODO generics? for the type?
    try {
      final JsonObject requestJson = context.getBodyAsJson();
      id = id(requestJson);
      request = requestJson.mapTo(SignTransactionJsonRpcRequest.class);
    } catch (final IllegalArgumentException exception) {
      return errorResponse(id);
    }

    final String signedTransactionHexString = signer.signTransaction(request);

    // TODO Pojo this!
    final JsonObject jsonObj = new JsonObject();
    jsonObj.put("jsonrpc", "2.0");
    jsonObj.put("method", "eth_sendRawTransaction");
    jsonObj.put("params", new JsonArray().add(signedTransactionHexString));
    jsonObj.put("id", id);

    return Buffer.buffer(jsonObj.toString());
  }

  private Buffer errorResponse(final Object id) {
    return Json.encodeToBuffer(new JsonRpcErrorResponse(id, JsonRpcError.INVALID_REQUEST));
  }

  private Object id(final JsonObject requestJson) {
    return new JsonRpcRequestId(requestJson.getValue("id")).getValue();
  }
}

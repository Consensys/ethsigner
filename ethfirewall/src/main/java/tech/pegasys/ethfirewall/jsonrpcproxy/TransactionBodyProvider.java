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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TransactionBodyProvider implements BodyProvider {

  private final TransactionSigner transactionSigner;

  public TransactionBodyProvider(final TransactionSigner transactionSigner) {
    this.transactionSigner = transactionSigner;
  }

  @Override
  public Buffer getBody(RoutingContext context) {
    final String signedTransactionHexString =
        transactionSigner.signTransaction(context.getBodyAsJson());

    final int originalId = context.getBodyAsJson().getInteger("id");

    final JsonObject jsonObj = new JsonObject();
    jsonObj.put("jsonrpc", "2.0");
    jsonObj.put("method", "eth_sendRawTransaction");
    jsonObj.put("params", new JsonArray().add(signedTransactionHexString));
    jsonObj.put("id", originalId);

    return Buffer.buffer(jsonObj.toString());
  }
}

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

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;

import io.vertx.core.http.HttpServerRequest;

public class SendTransactionContext {
  private final HttpServerRequest initialRequest;
  private final JsonRpcRequestId id;
  private Transaction transaction;
  private Runnable preTransmitOperation;

  public SendTransactionContext(
      final HttpServerRequest initialRequest,
      final JsonRpcRequestId id,
      final Transaction transaction,
      Runnable preTransmitOperation) {
    this.initialRequest = initialRequest;
    this.id = id;
    this.transaction = transaction;
    this.preTransmitOperation = preTransmitOperation;
  }

  public HttpServerRequest getInitialRequest() {
    return initialRequest;
  }

  public JsonRpcRequestId getId() {
    return id;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public Runnable getPreTransmitOperation() {
    return preTransmitOperation;
  }
}

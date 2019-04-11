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

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;

import io.vertx.core.http.HttpServerRequest;

public class TransactionInformation {
  private final HttpServerRequest initialRequest;
  private final JsonRpcRequestId receivedId;
  private final RawTransactionSupplier rawTransactionSupplier;

  public TransactionInformation(
      HttpServerRequest initialRequest,
      RawTransactionSupplier rawTransactionSupplier,
      JsonRpcRequestId receivedId) {
    this.initialRequest = initialRequest;
    this.rawTransactionSupplier = rawTransactionSupplier;
    this.receivedId = receivedId;
  }

  public HttpServerRequest getInitialRequest() {
    return initialRequest;
  }

  public RawTransactionSupplier getRawTransactionSupplier() {
    return rawTransactionSupplier;
  }

  public JsonRpcRequestId getReceivedId() {
    return receivedId;
  }
}

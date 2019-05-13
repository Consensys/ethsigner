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

import ethsigner.jsonrpc.JsonRpcRequestId;
import io.vertx.core.http.HttpServerRequest;

public class SendTransactionContext {
  private final HttpServerRequest initialRequest;
  private final JsonRpcRequestId id;
  private final RawTransactionBuilder rawTransactionBuilder;

  public SendTransactionContext(
      final HttpServerRequest initialRequest,
      final RawTransactionBuilder rawTransactionBuilder,
      final JsonRpcRequestId id) {
    this.initialRequest = initialRequest;
    this.rawTransactionBuilder = rawTransactionBuilder;
    this.id = id;
  }

  public HttpServerRequest getInitialRequest() {
    return initialRequest;
  }

  public RawTransactionBuilder getRawTransactionBuilder() {
    return rawTransactionBuilder;
  }

  public JsonRpcRequestId getId() {
    return id;
  }
}

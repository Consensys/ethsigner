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
package tech.pegasys.ethsigner.requesthandler;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;

import io.vertx.core.buffer.Buffer;

public class JsonRpcBody {

  private final JsonRpcErrorResponse error;
  private final Buffer body;

  public JsonRpcBody(final JsonRpcErrorResponse error) {
    this.body = null;
    this.error = error;
  }

  public JsonRpcBody(final Buffer body) {
    this.body = body;
    this.error = null;
  }

  public boolean hasError() {
    return error != null;
  }

  /**
   * JSON-RPC body.
   *
   * @return <code>null</code> when an error was encountered.
   */
  public Buffer body() {
    return body;
  }

  /**
   * Any error encountered whilst attempting to create the body.
   *
   * @return error, may be <code>null</code>
   */
  public JsonRpcErrorResponse error() {
    return error;
  }
}

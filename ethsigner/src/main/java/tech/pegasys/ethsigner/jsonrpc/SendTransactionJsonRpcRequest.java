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
package tech.pegasys.ethsigner.jsonrpc;

import tech.pegasys.ethsigner.jsonrpc.exception.InvalidJsonRpcRequestException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Objects;

public class SendTransactionJsonRpcRequest {

  private JsonRpcRequestId id;
  private final String method;
  private final SendTransactionJsonParameters params;
  private final String version;

  @JsonCreator
  public SendTransactionJsonRpcRequest(
      @JsonProperty("jsonrpc") final String version,
      @JsonProperty("method") final String method,
      @JsonProperty("params") final SendTransactionJsonParameters[] params) {
    this.version = version;
    this.method = method;

    if (method == null) {
      throw new InvalidJsonRpcRequestException("Field 'method' is required");
    }

    if (params == null || params.length != 1) {
      throw new InvalidJsonRpcRequestException(
          "Field 'params' is required and must be an array of length one");
    }

    this.params = params[0];
  }

  public Object getId() {
    return id == null ? null : id.getValue();
  }

  public String getMethod() {
    return method;
  }

  public String getVersion() {
    return version;
  }

  public SendTransactionJsonParameters getParams() {
    return params;
  }

  @JsonSetter("id")
  protected void setId(final JsonRpcRequestId id) {
    this.id = id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SendTransactionJsonRpcRequest that = (SendTransactionJsonRpcRequest) o;
    return Objects.equal(id, that.id)
        && Objects.equal(method, that.method)
        && Objects.equal(params, that.params)
        && Objects.equal(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, method, params, version);
  }
}

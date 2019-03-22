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
package tech.pegasys.ethfirewall.jsonrpc;

import tech.pegasys.ethfirewall.jsonrpc.exception.InvalidJsonRpcRequestException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Objects;

public class SignTransactionJsonRpcRequest {

  private JsonRpcRequestId id;
  private final String method;
  private final SignTransactionJsonParameters params;
  private final String version;

  @JsonCreator
  public SignTransactionJsonRpcRequest(
      @JsonProperty("jsonrpc") final String version,
      @JsonProperty("method") final String method,
      @JsonProperty("params") final SignTransactionJsonParameters params) {
    this.version = version;
    this.method = method;
    this.params = params;
    if (method == null) {
      throw new InvalidJsonRpcRequestException("Field 'method' is required");
    }
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

  public SignTransactionJsonParameters getParams() {
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
    final SignTransactionJsonRpcRequest that = (SignTransactionJsonRpcRequest) o;
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

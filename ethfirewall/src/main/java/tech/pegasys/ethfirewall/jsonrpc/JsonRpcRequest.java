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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Objects;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonRpcRequest {

  private JsonRpcRequestId id;
  private final String method;
  private final Object params;
  private final String version;

  @JsonCreator
  public JsonRpcRequest(
      @JsonProperty("jsonrpc") final String version,
      @JsonProperty("method") final String method,
      @JsonProperty("params") final Object params) {
    this.version = version;
    this.method = method;
    this.params = params;
    if (method == null) {
      throw new InvalidJsonRpcRequestException("Field 'method' is required");
    }
  }

  @JsonGetter("id")
  public JsonRpcRequestId getId() {
    return id;
  }

  @JsonGetter("method")
  public String getMethod() {
    return method;
  }

  @JsonGetter("jsonrpc")
  public String getVersion() {
    return version;
  }

  @JsonInclude(Include.NON_NULL)
  @JsonGetter("params")
  private Object getRawParams() {
    return params;
  }

  public Object getParams() {
    if (params instanceof List) {
      JsonArray jsonArray = new JsonArray((List) params);
      if (jsonArray.isEmpty()) {
        return null;
      }
      return jsonArray.getValue(0);
    }
    return params;
  }

  @JsonSetter("id")
  public void setId(final JsonRpcRequestId id) {
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
    final JsonRpcRequest that = (JsonRpcRequest) o;

    return isParamsEqual(that.params)
        && Objects.equal(id, that.id)
        && Objects.equal(method, that.method)
        && Objects.equal(version, that.version);
  }

  private boolean isParamsEqual(final Object otherParams) {
    if (params.getClass().isArray()) {
      if (!otherParams.getClass().isArray()) {
        return false;
      }
      Object[] paramsArray = (Object[]) params;
      Object[] thatParamsArray = (Object[]) otherParams;
      return Arrays.equals(paramsArray, thatParamsArray);
    } else if (otherParams.getClass().isArray()) {
      return false;
    }

    return params.equals(otherParams);
  }

  @Override
  public int hashCode() {
    final int paramsHashCode;
    if (params.getClass().isArray()) {
      paramsHashCode = Arrays.hashCode((Object[]) params);
    } else {
      paramsHashCode = params.hashCode();
    }
    return Objects.hashCode(id, method, paramsHashCode, version);
  }

  public static JsonRpcRequest convertFrom(final JsonObject jsonObject) {
    final JsonRpcRequest result = jsonObject.mapTo(JsonRpcRequest.class);

    final Object params = result.getRawParams();
    if (params != null) {
      if (params instanceof ArrayList) {
        JsonArray jsonArray = new JsonArray((ArrayList) params);

        if (jsonArray.size() > 1) {
          throw new IllegalArgumentException("Illegally constructed Transaction Json content.");
        }
      }
    }

    return result;
  }
}

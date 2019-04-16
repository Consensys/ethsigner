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
package tech.pegasys.ethsigner.requesthandler.internalresponse;

import static java.util.Collections.singletonList;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcSuccessResponse;
import tech.pegasys.ethsigner.requesthandler.BodyProvider;
import tech.pegasys.ethsigner.requesthandler.JsonRpcBody;

import java.util.Collection;

import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthAccountsBodyProvider implements BodyProvider {

  private static final Logger LOG = LoggerFactory.getLogger(EthAccountsBodyProvider.class);

  private final String address;

  public EthAccountsBodyProvider(final String address) {
    this.address = address;
  }

  @Override
  public JsonRpcBody getBody(final JsonRpcRequest request) {
    final Object params = request.getParams();

    if (isPopulated(params) && isNotEmptyArray(params)) {
      LOG.info("eth_accounts should have no parameters, but has {}", request.getParams());
      return new JsonRpcBody(
          new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS));
    }

    final JsonRpcSuccessResponse response =
        new JsonRpcSuccessResponse(request.getId(), singletonList(address));
    return new JsonRpcBody(Json.encodeToBuffer(response));
  }

  private boolean isPopulated(final Object params) {
    return params != null;
  }

  private boolean isNotEmptyArray(final Object params) {
    boolean arrayIsEmpty = false;
    boolean paramsIsArray = (params instanceof Collection);
    if (paramsIsArray) {
      Collection<?> collection = (Collection<?>) params;
      arrayIsEmpty = collection.isEmpty();
    }

    return !(paramsIsArray && arrayIsEmpty);
  }
}

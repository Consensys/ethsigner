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
package tech.pegasys.ethsigner.core.requesthandler.internalresponse;

import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcSuccessResponse;
import tech.pegasys.ethsigner.core.requesthandler.BodyProvider;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EthAccountsBodyProvider implements BodyProvider {

  private final Supplier<Set<String>> addressesSupplier;

  public EthAccountsBodyProvider(final Supplier<Set<String>> addressesSupplier) {
    this.addressesSupplier = addressesSupplier;
  }

  @Override
  public JsonRpcSuccessResponse getBody(final JsonRpcRequest request) {
    final Object params = request.getParams();

    if (isPopulated(params) && isNotEmptyArray(params)) {
      throw new JsonRpcException(new JsonRpcErrorResponse(request.getId(), INVALID_PARAMS));
    }

    final List<String> addresses =
        addressesSupplier.get().stream().sorted().collect(Collectors.toList());
    return new JsonRpcSuccessResponse(request.getId(), addresses);
  }

  private boolean isPopulated(final Object params) {
    return params != null;
  }

  private boolean isNotEmptyArray(final Object params) {
    boolean arrayIsEmpty = false;
    final boolean paramsIsArray = (params instanceof Collection);
    if (paramsIsArray) {
      final Collection<?> collection = (Collection<?>) params;
      arrayIsEmpty = collection.isEmpty();
    }

    return !(paramsIsArray && arrayIsEmpty);
  }
}

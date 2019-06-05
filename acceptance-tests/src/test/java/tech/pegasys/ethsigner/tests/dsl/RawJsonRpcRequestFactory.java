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
package tech.pegasys.ethsigner.tests.dsl;

import java.util.Collections;
import java.util.List;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

public class RawJsonRpcRequestFactory {

  public static class ArbitraryResponseType extends Response<Boolean> {}

  private final Web3jService web3jService;

  public RawJsonRpcRequestFactory(final Web3jService web3jService) {
    this.web3jService = web3jService;
  }

  public Request<?, ArbitraryResponseType> createRequest(final String method) {
    return new Request<>(
        method, Collections.emptyList(), web3jService, ArbitraryResponseType.class);
  }

  public <S, T extends Response<?>> Request<S, T> createRequest(
      final String method, final List<S> params, final Class<T> type) {
    return new Request<>(method, params, web3jService, type);
  }
}

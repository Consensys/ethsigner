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
package tech.pegasys.ethsigner.core.jsonrpcproxy;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.internalresponse.EthAccountsResultProvider;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class EthAccountsResultProviderTest {

  @Test
  public void valueFromBodyProviderInsertedToResult() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(address));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(emptyList());

    final Object body = resultProvider.createResponseResult(request);

    assertThat(body).isInstanceOf(List.class);
    final List<String> addressses = (List<String>) body;
    assertThat(addressses).containsExactly(address);
  }

  @Test
  public void ifParamsContainsANonEmptyArrayExceptionIsThrownWithInvalidParams() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(address));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(singletonList(5));

    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void ifParamIsAnObjectExceptionIsThrownWithInvalidParams() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(address));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(5);

    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void missingParametersIsOk() {
    final String address = "MyAddress";
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(address));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));

    final Object body = resultProvider.createResponseResult(request);
    assertThat(body).isInstanceOf(List.class);
    final List<String> addressses = (List<String>) body;
    assertThat(addressses).containsExactly(address);
  }

  @Test
  public void multipleValueFromBodyProviderInsertedToResult() {
    final Set<String> availableAddresses = Set.of("a", "b", "c");
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> availableAddresses);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(emptyList());

    final Object body = resultProvider.createResponseResult(request);

    assertThat(body).isInstanceOf(List.class);
    final List<String> reportedAddresses = (List<String>) body;
    assertThat(reportedAddresses).containsExactlyInAnyOrderElementsOf(availableAddresses);
  }

  @Test
  public void accountsReturnedAreDynamicallyFetchedFromProvider() {
    final Set<String> addresses = Sets.newHashSet("a", "b", "c");

    final Supplier<Set<String>> supplier = () -> addresses;
    final EthAccountsResultProvider resultProvider = new EthAccountsResultProvider(supplier);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(emptyList());

    Object body = resultProvider.createResponseResult(request);
    assertThat(body).isInstanceOf(List.class);
    List<String> reportedAddresses = (List<String>) body;
    assertThat(reportedAddresses).containsExactly("a", "b", "c");

    addresses.remove("a");

    body = resultProvider.createResponseResult(request);
    assertThat(body).isInstanceOf(List.class);
    reportedAddresses = (List<String>) body;
    assertThat(reportedAddresses).containsExactly("b", "c");
  }

  @Test
  public void accountsReturnedAreSortedAlphabetically() {
    final Supplier<Set<String>> supplier = () -> Sets.newHashSet("c", "b", "a");
    final EthAccountsResultProvider resultProvider = new EthAccountsResultProvider(supplier);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(emptyList());

    final Object body = resultProvider.createResponseResult(request);
    assertThat(body).isInstanceOf(List.class);
    List<String> reportedAddresses = (List<String>) body;
    assertThat(reportedAddresses).containsExactly("a", "b", "c");
  }
}

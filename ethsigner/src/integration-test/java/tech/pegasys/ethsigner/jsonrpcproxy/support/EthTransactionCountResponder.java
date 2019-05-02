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
package tech.pegasys.ethsigner.jsonrpcproxy.support;

import static org.mockserver.model.HttpResponse.response;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcSuccessResponse;

import java.math.BigInteger;
import java.util.function.Function;

import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RegexBody;

public class EthTransactionCountResponder implements ExpectationResponseCallback {

  private static final Logger LOG = LogManager.getLogger();

  private static final String REQUEST_REGEX_PATTERN = ".*eth_getTransactionCount.*";

  private BigInteger nonce = BigInteger.ZERO;
  private Function<BigInteger, BigInteger> nonceMutator;

  public EthTransactionCountResponder(Function<BigInteger, BigInteger> nonceMutator) {
    this.nonceMutator = nonceMutator;
  }

  @Override
  public HttpResponse handle(final HttpRequest httpRequest) {
    final JsonRpcRequestId id = getRequestId(httpRequest.getBodyAsString());
    nonce = nonceMutator.apply(nonce);
    return response(generateTransactionCountResponse(id));
  }

  private static JsonRpcRequestId getRequestId(final String jsonBody) {
    final JsonRpcRequest jsonRpcRequest = Json.decodeValue(jsonBody, JsonRpcRequest.class);
    return jsonRpcRequest.getId();
  }

  protected String generateTransactionCountResponse(final JsonRpcRequestId id) {
    final JsonRpcSuccessResponse response =
        new JsonRpcSuccessResponse(id.getValue(), "0x" + nonce.toString());
    LOG.debug("Responding with Nonce of {}", nonce.toString());

    return Json.encode(response);
  }

  public HttpRequest request() {
    return HttpRequest.request()
        .withBody(new RegexBody(EthTransactionCountResponder.REQUEST_REGEX_PATTERN));
  }
}

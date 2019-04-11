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

import java.math.BigInteger;
import java.util.function.Function;

import io.vertx.core.json.JsonObject;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthTransactionCountResponder implements ExpectationResponseCallback {

  private static final Logger LOG = LoggerFactory.getLogger(EthTransactionCountResponder.class);

  public static final String REQUEST_REGEX_PATTERN = ".*eth_getTransactionCount.*";

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
    final JsonObject obj = new JsonObject(jsonBody);
    final JsonRpcRequest request = obj.mapTo(JsonRpcRequest.class);
    return request.getId();
  }

  protected String generateTransactionCountResponse(final JsonRpcRequestId id) {
    final JsonObject json = new JsonObject();
    LOG.debug("Responding with Nonce of {}", nonce.toString());
    json.put("id", id.getValue());
    json.put("jsonrpc", "2.0");
    json.put("result", "0x" + nonce.toString(16));

    return json.encode();
  }
}

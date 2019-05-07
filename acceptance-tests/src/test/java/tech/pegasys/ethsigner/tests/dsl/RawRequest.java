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

import static org.assertj.core.api.Assertions.fail;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.Web3jHelpers;
import tech.pegasys.ethsigner.tests.dsl.RawJsonRpcRequestFactory.ArbitraryResponseType;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaderValues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.protocol.http.HttpService;

public class RawRequest {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpService web3jHttpService;
  private final RawJsonRpcRequestFactory requestFactory;

  public RawRequest(HttpService web3jHttpService, RawJsonRpcRequestFactory requestFactory) {
    this.web3jHttpService = web3jHttpService;
    this.requestFactory = requestFactory;
  }

  public SignerResponse<JsonRpcErrorResponse> exceptionalRequest(
      final String method, final Map<String, String> additionalHeaders) throws IOException {

    web3jHttpService.getHeaders().clear();
    web3jHttpService.addHeaders(additionalHeaders);
    web3jHttpService.addHeader("Content", HttpHeaderValues.APPLICATION_JSON.toString());

    final Request<?, ArbitraryResponseType> request = requestFactory.createRequest(method);

    try {
      request.send();
      fail("Expecting exceptional response ");
      return null;
    } catch (final ClientConnectionException e) {
      LOG.info("ClientConnectionException with message: " + e.getMessage());
      return Web3jHelpers.parseException(e);
    }
  }

  public SignerResponse<JsonRpcErrorResponse> exceptionalRequest(final String method)
      throws IOException {
    return exceptionalRequest(method, Collections.emptyMap());
  }
}

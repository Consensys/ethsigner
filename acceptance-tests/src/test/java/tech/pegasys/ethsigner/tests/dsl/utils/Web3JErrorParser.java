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
package tech.pegasys.ethsigner.tests.dsl.utils;

import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.web3j.protocol.exceptions.ClientConnectionException;

public class Web3JErrorParser {
  private static final Pattern WEB3J_EXCEPTION_MSG_PATTERN = Pattern.compile(".*:\\s(\\d+);(.*)");

  public static SignerResponse<JsonRpcErrorResponse> parseConnectionException(
      final ClientConnectionException e) {
    final Matcher matcher = WEB3J_EXCEPTION_MSG_PATTERN.matcher(e.getMessage());
    if (matcher.matches()) {
      final int statusCode = Integer.parseInt(matcher.group(1));
      final HttpResponseStatus status = HttpResponseStatus.valueOf(statusCode);
      final String jsonBody = matcher.group(2);
      final JsonRpcErrorResponse jsonRpcResponse =
          Json.decodeValue(jsonBody, JsonRpcErrorResponse.class);
      return new SignerResponse<>(jsonRpcResponse, status);
    } else {
      throw new RuntimeException("Unable to parse web3j exception message");
    }
  }
}

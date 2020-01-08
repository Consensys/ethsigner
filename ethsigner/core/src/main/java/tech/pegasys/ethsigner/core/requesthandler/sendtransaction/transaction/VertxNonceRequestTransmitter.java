/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcSuccessResponse;

import java.math.BigInteger;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.exceptions.MessageDecodingException;
import org.web3j.utils.Numeric;

public class VertxNonceRequestTransmitter {

  private static final Logger LOG = LogManager.getLogger();

  private final MultiMap headers;
  private final HttpClient client;
  private final JsonDecoder decoder;
  private final Duration requestTimeout;

  public VertxNonceRequestTransmitter(
      final MultiMap headers,
      final HttpClient client,
      final JsonDecoder decoder,
      final Duration requestTimeout) {
    this.headers = headers;
    this.client = client;
    this.decoder = decoder;
    this.requestTimeout = requestTimeout;
  }

  public BigInteger requestNonce(final JsonRpcRequest request) {
    final CompletableFuture<BigInteger> result = getNonceFromWeb3Provider(request, headers);

    try {
      final BigInteger nonce = result.get();
      LOG.debug("Supplying nonce of {}", nonce.toString());
      return nonce;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to retrieve nonce:" + e.getMessage(), e.getCause());
    }
  }

  private CompletableFuture<BigInteger> getNonceFromWeb3Provider(
      final JsonRpcRequest requestBody, final MultiMap headers) {

    final CompletableFuture<BigInteger> result = new CompletableFuture<>();

    final HttpClientRequest request =
        client.request(
            HttpMethod.POST,
            "/",
            response -> response.bodyHandler(responseBody -> handleResponse(responseBody, result)));

    request.setTimeout(requestTimeout.toMillis());
    request.headers().setAll(headers);
    request.exceptionHandler(result::completeExceptionally);
    request.headers().remove("Content-Length"); // created during 'end'.
    request.setChunked(false);
    request.end(Json.encode(requestBody));
    LOG.info("Transmitted {}", Json.encode(requestBody));

    return result;
  }

  private void handleResponse(final Buffer bodyBuffer, final CompletableFuture<BigInteger> result) {
    try {

      final JsonRpcSuccessResponse response =
          decoder.decodeValue(bodyBuffer, JsonRpcSuccessResponse.class);
      final Object suppliedNonce = response.getResult();
      if (suppliedNonce instanceof String) {
        try {
          result.complete(Numeric.decodeQuantity((String) suppliedNonce));
          return;
        } catch (final MessageDecodingException ex) {
          result.completeExceptionally(ex);
          return;
        }
      }
      result.completeExceptionally(new RuntimeException("Web3 did not provide a string response."));
    } catch (final DecodeException e) {
      result.completeExceptionally(
          new RuntimeException(
              "Web3 Provider did not respond with a valid success message: "
                  + bodyBuffer.toString(UTF_8)));
    }
  }
}

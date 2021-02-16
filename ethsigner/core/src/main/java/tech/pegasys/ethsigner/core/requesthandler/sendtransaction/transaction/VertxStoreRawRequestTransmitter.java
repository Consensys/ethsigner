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

import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.determineErrorCode;

import tech.pegasys.ethsigner.core.http.HeaderHelpers;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcSuccessResponse;
import tech.pegasys.ethsigner.core.requesthandler.DownstreamResponseHandler;
import tech.pegasys.ethsigner.core.requesthandler.RequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.exceptions.MessageDecodingException;

public class VertxStoreRawRequestTransmitter {

  private static final Logger LOG = LogManager.getLogger();

  private final MultiMap headers;
  private final JsonDecoder decoder;
  private final VertxRequestTransmitterFactory transmitterFactory;

  private static final AtomicInteger nextId = new AtomicInteger(0);

  public VertxStoreRawRequestTransmitter(
      final MultiMap headers,
      final JsonDecoder decoder,
      final VertxRequestTransmitterFactory transmitterFactory) {
    this.headers = headers;
    this.transmitterFactory = transmitterFactory;
    this.decoder = decoder;
  }

  public String storeRaw(final JsonRpcRequest request) {
    final CompletableFuture<String> result = storePayloadAndGetLookupId(request, headers);

    try {
      final String lookupId = result.get();
      LOG.debug("storeRaw response of {}", lookupId);
      return lookupId;
    } catch (final InterruptedException | ExecutionException e) {
      throw new RuntimeException(
          "Failed to retrieve storeRaw result (enclave lookup id):" + e.getMessage(), e.getCause());
    }
  }

  private CompletableFuture<String> storePayloadAndGetLookupId(
      final JsonRpcRequest requestBody, final MultiMap headers) {

    final CompletableFuture<String> result = new CompletableFuture<>();

    final RequestTransmitter transmitter = transmitterFactory.create(new ResponseCallback(result));

    final MultiMap headersToSend = HeaderHelpers.createHeaders(headers);
    requestBody.setId(new JsonRpcRequestId(nextId.getAndIncrement()));
    transmitter.sendRequest(HttpMethod.POST, headersToSend, "/", Json.encode(requestBody));

    LOG.info("Transmitted {}", Json.encode(requestBody));

    return result;
  }

  private void handleResponse(final String body, final CompletableFuture<String> result) {
    try {

      final JsonRpcSuccessResponse response =
          decoder.decodeValue(Buffer.buffer(body), JsonRpcSuccessResponse.class);
      final Object suppliedLookupId = response.getResult();
      if (suppliedLookupId instanceof String) {
        try {
          result.complete((String) suppliedLookupId);
          return;
        } catch (final MessageDecodingException ex) {
          result.completeExceptionally(ex);
          return;
        }
      }
      result.completeExceptionally(new RuntimeException("Web3 did not provide a string response."));
    } catch (final DecodeException e) {
      result.completeExceptionally(new JsonRpcException(determineErrorCode(body, decoder)));
    }
  }

  private class ResponseCallback implements DownstreamResponseHandler {
    private final CompletableFuture<String> result;

    private ResponseCallback(final CompletableFuture<String> result) {
      this.result = result;
    }

    @Override
    public void handleResponse(
        final Iterable<Entry<String, String>> headers, final int statusCode, String body) {
      VertxStoreRawRequestTransmitter.this.handleResponse(body, result);
    }

    @Override
    public void handleFailure(Throwable t) {
      result.completeExceptionally(t);
    }
  }
}

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
package tech.pegasys.ethsigner.requesthandler.sendtransaction;

import static java.util.Collections.singletonList;

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.requesthandler.JsonRpcErrorReporter;
import tech.pegasys.ethsigner.signing.TransactionSigner;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class TransactionPassthrough {

  private static final Logger LOG = LoggerFactory.getLogger(TransactionPassthrough.class);
  private static final String JSON_RPC_VERSION = "2.0";
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";

  private final HttpClient ethNodeClient;
  private final TransactionSigner signer;
  protected final TransactionInformation transactionInfo;

  public TransactionPassthrough(
      final HttpClient ethNodeClient,
      final TransactionInformation transactionInfo,
      final TransactionSigner signer) {
    this.ethNodeClient = ethNodeClient;
    this.transactionInfo = transactionInfo;
    this.signer = signer;
  }

  public void send() {
    final JsonRpcBody body = getBody();
    if (body.hasError()) {
    } else {
      final HttpServerRequest httpServerRequest = transactionInfo.getInitialRequest();
      final HttpClientRequest proxyRequest =
          ethNodeClient.request(
              httpServerRequest.method(), httpServerRequest.uri(), this::handleResponse);

      proxyRequest.headers().setAll(httpServerRequest.headers());
      proxyRequest.headers().remove("Content-Length"); // created during 'end'.
      proxyRequest.setChunked(false);
      proxyRequest.end(body.body());
    }
  }

  private JsonRpcBody getBody() {
    // This assumes the parameters have already been validated and are correct for the unlocked
    // account.
    final String signedTransactionHexString;
    try {
      final RawTransaction rawTransaction = transactionInfo.getRawTransactionSupplier().get();
      signedTransactionHexString = signer.signTransaction(rawTransaction);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Bad input value from request: {}", "UNKNOWN", e);
      return createJsonRpcBodyFrom(JsonRpcError.INVALID_PARAMS);
    } catch (final Throwable e) {
      LOG.debug("Unhandled error processing request: {}", "UNKNOWN", e);
      return createJsonRpcBodyFrom(JsonRpcError.INTERNAL_ERROR);
    }

    final JsonRpcRequest sendRawTransaction =
        new JsonRpcRequest(
            JSON_RPC_VERSION, JSON_RPC_METHOD, singletonList(signedTransactionHexString));
    sendRawTransaction.setId(transactionInfo.getReceivedId());

    try {
      return new JsonRpcBody(Json.encodeToBuffer(sendRawTransaction));
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Serialisation failed for: {}", sendRawTransaction, e);
      return createJsonRpcBodyFrom(JsonRpcError.INTERNAL_ERROR);
    }
  }

  private JsonRpcBody createJsonRpcBodyFrom(final JsonRpcError error) {
    return new JsonRpcBody(new JsonRpcErrorResponse(transactionInfo.getReceivedId(), error));
  }

  private void handleResponse(final HttpClientResponse response) {
    logResponse(response);
    response.bodyHandler(
        body -> {
          logResponseBody(body);
          handleResponseBody(response, body);
        });
  }

  protected void handleResponseBody(final HttpClientResponse response, final Buffer body) {
    final HttpServerRequest httpServerRequest = transactionInfo.getInitialRequest();
    httpServerRequest.response().setStatusCode(response.statusCode());
    httpServerRequest.response().headers().setAll(response.headers());
    httpServerRequest.response().setChunked(false);
    httpServerRequest.response().end(body);
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }
}

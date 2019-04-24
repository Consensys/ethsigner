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

import tech.pegasys.ethsigner.http.HttpResponseFactory;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.SendTransactionJsonParameters;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.requesthandler.JsonRpcRequestHandler;
import tech.pegasys.ethsigner.signing.TransactionSerialiser;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendTransactionHandler extends JsonRpcRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SendTransactionHandler.class);

  private final HttpClient ethNodeClient;
  private final TransactionSerialiser serialiser;
  private final NonceProvider nonceProvider;

  public SendTransactionHandler(
      final HttpResponseFactory responder,
      final HttpClient ethNodeClient,
      final TransactionSerialiser serialiser,
      final NonceProvider nonceProvider) {
    super(responder);
    this.ethNodeClient = ethNodeClient;
    this.serialiser = serialiser;
    this.nonceProvider = nonceProvider;
  }

  @Override
  public void handle(final HttpServerRequest httpServerRequest, final JsonRpcRequest request) {
    final SendTransactionJsonParameters params;
    try {
      params = SendTransactionJsonParameters.from(request);
    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      reportError(httpServerRequest, request, JsonRpcError.INVALID_PARAMS);
      return;
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Deserialisation failed for request: {}", request.getParams(), e);
      reportError(httpServerRequest, request, JsonRpcError.INVALID_PARAMS);
      return;
    }

    if (senderNotUnlockedAccount(params)) {
      LOG.info(
          "From address ({}) does not match unlocked account ({})",
          params.sender(),
          serialiser.getAddress());
      reportError(httpServerRequest, request, JsonRpcError.INVALID_PARAMS);
      return;
    }

    try {
      sendTransaction(params, httpServerRequest, request);
    } catch (final RuntimeException e) {
      LOG.info("Unable to get nonce from web3j provider.");
      reportError(httpServerRequest, request, JsonRpcError.INTERNAL_ERROR);
    }
  }

  private void sendTransaction(
      final SendTransactionJsonParameters params,
      final HttpServerRequest httpServerRequest,
      final JsonRpcRequest request) {

    final TransactionTransmitter transmitter =
        createTransactionTransmitter(params, httpServerRequest, request);

    transmitter.send();
  }

  private TransactionTransmitter createTransactionTransmitter(
      final SendTransactionJsonParameters params,
      final HttpServerRequest httpServerRequest,
      final JsonRpcRequest request) {

    final RawTransactionBuilder transactionBuilder = RawTransactionBuilder.from(params);
    final SendTransactionContext context =
        new SendTransactionContext(httpServerRequest, transactionBuilder, request.getId());

    final RetryMechanism<SendTransactionContext> retryMechanism;

    if (!params.nonce().isPresent()) {
      transactionBuilder.updateNonce(nonceProvider.getNonce());
      retryMechanism = new NonceTooLowRetryMechanism(nonceProvider);
    } else {
      retryMechanism = new NoRetryMechanism<>();
    }

    return new TransactionTransmitter(
        ethNodeClient, context, serialiser, retryMechanism, responder);
  }

  private boolean senderNotUnlockedAccount(final SendTransactionJsonParameters params) {
    return !params.sender().equalsIgnoreCase(serialiser.getAddress());
  }
}

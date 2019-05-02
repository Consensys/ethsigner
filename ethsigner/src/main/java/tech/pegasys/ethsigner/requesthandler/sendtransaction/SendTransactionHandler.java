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

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.ethsigner.http.HttpResponseFactory;
import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.SendTransactionJsonParameters;
import tech.pegasys.ethsigner.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.requesthandler.JsonRpcRequestHandler;
import tech.pegasys.ethsigner.signing.TransactionSerialiser;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendTransactionHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private HttpResponseFactory responder;
  private final HttpClient ethNodeClient;
  private final TransactionSerialiser serialiser;
  private final NonceProvider nonceProvider;

  public SendTransactionHandler(
      final HttpResponseFactory responder,
      final HttpClient ethNodeClient,
      final TransactionSerialiser serialiser,
      final NonceProvider nonceProvider) {
    this.responder = responder;
    this.ethNodeClient = ethNodeClient;
    this.serialiser = serialiser;
    this.nonceProvider = nonceProvider;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    LOG.debug("Transforming request {}, {}", request.getId(), request.getMethod());
    final SendTransactionJsonParameters params;
    try {
      params = SendTransactionJsonParameters.from(request);
    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      context.fail(BAD_REQUEST.code(), new JsonRpcException(INVALID_PARAMS));
      return;
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Deserialisation failed for request: {}", request.getParams(), e);
      context.fail(BAD_REQUEST.code(), new JsonRpcException(INVALID_PARAMS));
      return;
    }

    if (senderNotUnlockedAccount(params)) {
      LOG.info(
          "From address ({}) does not match unlocked account ({})",
          params.sender(),
          serialiser.getAddress());
      context.fail(
          BAD_REQUEST.code(), new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
      return;
    }

    try {
      sendTransaction(params, context.request(), request);
    } catch (final RuntimeException e) {
      LOG.info("Unable to get nonce from web3j provider.");
      final Throwable cause = e.getCause();
      if (cause instanceof SocketException || cause instanceof SocketTimeoutException) {
        context.fail(
            GATEWAY_TIMEOUT.code(), new JsonRpcException(CONNECTION_TO_DOWNSTREAM_NODE_TIMED_OUT));
      } else {
        context.fail(GATEWAY_TIMEOUT.code(), new JsonRpcException(INTERNAL_ERROR));
      }
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
      LOG.debug("Nonce not present in request {}", request.getId());
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

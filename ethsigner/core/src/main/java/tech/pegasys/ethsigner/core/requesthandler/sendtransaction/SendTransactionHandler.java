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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcRequestHandler;
import tech.pegasys.ethsigner.core.signing.TransactionSerialiser;

import java.time.Duration;

import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendTransactionHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpResponseFactory responder;
  private final HttpClient ethNodeClient;
  private final TransactionSerialiser serialiser;
  private final NonceProvider nonceProvider;
  private final TransactionFactory transactionFactory;
  private final Duration httpRequestTimeout;

  public SendTransactionHandler(
      final HttpResponseFactory responder,
      final HttpClient ethNodeClient,
      final TransactionSerialiser serialiser,
      final NonceProvider nonceProvider,
      final TransactionFactory transactionFactory,
      final Duration httpRequestTimeout) {
    this.responder = responder;
    this.ethNodeClient = ethNodeClient;
    this.serialiser = serialiser;
    this.nonceProvider = nonceProvider;
    this.transactionFactory = transactionFactory;
    this.httpRequestTimeout = httpRequestTimeout;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    LOG.debug("Transforming request {}, {}", request.getId(), request.getMethod());
    final Transaction transaction;
    try {
      transaction = transactionFactory.createTransaction(request);
    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      context.fail(BAD_REQUEST.code(), new JsonRpcException(INVALID_PARAMS));
      return;
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Deserialisation failed for request: {}", request.getParams(), e);
      context.fail(BAD_REQUEST.code(), new JsonRpcException(INVALID_PARAMS));
      return;
    }

    if (senderNotUnlockedAccount(transaction)) {
      LOG.info(
          "From address ({}) does not match unlocked account ({})",
          transaction.sender(),
          serialiser.getAddress());
      context.fail(
          BAD_REQUEST.code(), new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
      return;
    }

    sendTransaction(transaction, context, request);
  }

  private void sendTransaction(
      final Transaction transaction,
      final RoutingContext routingContext,
      final JsonRpcRequest request) {
    final TransactionTransmitter transmitter =
        createTransactionTransmitter(transaction, routingContext, request);
    transmitter.send();
  }

  private TransactionTransmitter createTransactionTransmitter(
      final Transaction transaction,
      final RoutingContext routingContext,
      final JsonRpcRequest request) {

    final Runnable preTransmitOperation;
    final RetryMechanism<SendTransactionContext> retryMechanism;

    if (!transaction.hasNonce()) {
      LOG.debug("Nonce not present in request {}", request.getId());
      preTransmitOperation = () -> transaction.updateNonce(nonceProvider.getNonce());
      retryMechanism = new NonceTooLowRetryMechanism();
    } else {
      retryMechanism = new NoRetryMechanism<>();
      preTransmitOperation = () -> {};
    }

    final SendTransactionContext context =
        new SendTransactionContext(
            routingContext, request.getId(), transaction, preTransmitOperation);

    return new TransactionTransmitter(
        ethNodeClient, context, serialiser, retryMechanism, responder, httpRequestTimeout);
  }

  private boolean senderNotUnlockedAccount(final Transaction transaction) {
    return !transaction.sender().equalsIgnoreCase(serialiser.getAddress());
  }
}

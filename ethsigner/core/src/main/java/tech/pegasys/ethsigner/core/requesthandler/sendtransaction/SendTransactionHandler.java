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

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcRequestHandler;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.ethsigner.core.signing.TransactionSerializer;
import tech.pegasys.signers.secp256k1.api.TransactionSigner;
import tech.pegasys.signers.secp256k1.api.TransactionSignerProvider;

import java.util.Optional;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendTransactionHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final long chainId;
  private final HttpClient ethNodeClient;
  private final DownstreamPathCalculator downstreamPathCalculator;
  private final TransactionSignerProvider transactionSignerProvider;
  private final TransactionFactory transactionFactory;
  private final VertxRequestTransmitterFactory vertxTransmitterFactory;

  private static final int MAX_NONCE_RETRIES = 5;

  public SendTransactionHandler(
      final long chainId,
      final HttpClient ethNodeClient,
      final DownstreamPathCalculator downstreamPathCalculator,
      final TransactionSignerProvider transactionSignerProvider,
      final TransactionFactory transactionFactory,
      final VertxRequestTransmitterFactory vertxTransmitterFactory) {
    this.chainId = chainId;
    this.ethNodeClient = ethNodeClient;
    this.downstreamPathCalculator = downstreamPathCalculator;
    this.transactionSignerProvider = transactionSignerProvider;
    this.transactionFactory = transactionFactory;
    this.vertxTransmitterFactory = vertxTransmitterFactory;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    LOG.debug("Transforming request {}, {}", request.getId(), request.getMethod());
    final Transaction transaction;
    try {
      transaction = transactionFactory.createTransaction(context, request);
    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      context.fail(BAD_REQUEST.code(), new JsonRpcException(INVALID_PARAMS));
      return;
    } catch (final IllegalArgumentException | DecodeException e) {
      LOG.debug("JSON Deserialization failed for request: {}", request.getParams(), e);
      context.fail(BAD_REQUEST.code(), new JsonRpcException(INVALID_PARAMS));
      return;
    }

    final Optional<TransactionSigner> transactionSigner =
        transactionSignerProvider.getSigner(transaction.sender());

    if (transactionSigner.isEmpty()) {
      LOG.info("From address ({}) does not match any available account", transaction.sender());
      context.fail(
          BAD_REQUEST.code(), new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
      return;
    }

    final TransactionSerializer transactionSerializer =
        new TransactionSerializer(transactionSigner.get(), chainId);
    sendTransaction(transaction, transactionSerializer, context, request);
  }

  private void sendTransaction(
      final Transaction transaction,
      final TransactionSerializer transactionSerializer,
      final RoutingContext routingContext,
      final JsonRpcRequest request) {
    final TransactionTransmitter transmitter =
        createTransactionTransmitter(transaction, transactionSerializer, routingContext, request);
    transmitter.send();
  }

  private TransactionTransmitter createTransactionTransmitter(
      final Transaction transaction,
      final TransactionSerializer transactionSerializer,
      final RoutingContext routingContext,
      final JsonRpcRequest request) {

    if (!transaction.isNonceUserSpecified()) {
      LOG.debug("Nonce not present in request {}", request.getId());
      return new RetryingTransactionTransmitter(
          ethNodeClient,
          downstreamPathCalculator.calculateDownstreamPath(routingContext.request().uri()),
          transaction,
          transactionSerializer,
          vertxTransmitterFactory,
          new NonceTooLowRetryMechanism(MAX_NONCE_RETRIES),
          routingContext);
    } else {
      LOG.debug("Nonce supplied by client, forwarding request");
      return new TransactionTransmitter(
          ethNodeClient,
          downstreamPathCalculator.calculateDownstreamPath(routingContext.request().uri()),
          transaction,
          transactionSerializer,
          vertxTransmitterFactory,
          routingContext);
    }
  }
}

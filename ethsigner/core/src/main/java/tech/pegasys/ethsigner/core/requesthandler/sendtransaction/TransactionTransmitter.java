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
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import tech.pegasys.ethsigner.core.http.HeaderHelpers;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitter;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.ethsigner.core.signing.TransactionSerializer;

import java.util.Optional;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionTransmitter extends ForwardedMessageResponder {

  private static final Logger LOG = LogManager.getLogger();

  private final TransactionSerializer transactionSerializer;
  private final Transaction transaction;
  private final VertxRequestTransmitterFactory transmitterFactory;

  public TransactionTransmitter(
      final Transaction transaction,
      final TransactionSerializer transactionSerializer,
      final VertxRequestTransmitterFactory transmitterFactory,
      final RoutingContext context) {
    super(context);
    this.transmitterFactory = transmitterFactory;
    this.transaction = transaction;
    this.transactionSerializer = transactionSerializer;
  }

  public void send() {
    final Optional<JsonRpcRequest> request = createSignedTransactionBody();

    if (request.isEmpty()) {
      return;
    }

    try {
      sendTransaction(Json.encode(request.get()));
    } catch (final IllegalArgumentException | EncodeException e) {
      LOG.debug("JSON Serialization failed for: {}", request, e);
      context().fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
    }
  }

  private Optional<JsonRpcRequest> createSignedTransactionBody() {

    if (!transaction.isNonceUserSpecified()) {
      if (!populateNonce()) {
        return Optional.empty();
      }
    }

    final String signedTransactionHexString;
    try {
      signedTransactionHexString = transactionSerializer.serialize(transaction);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Failed to encode transaction: {}", transaction, e);
      context().fail(BAD_REQUEST.code(), new JsonRpcException(JsonRpcError.INVALID_PARAMS));
      return Optional.empty();
    } catch (final Throwable thrown) {
      LOG.debug("Failed to encode transaction: {}", transaction, thrown);
      context().fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
      return Optional.empty();
    }

    return Optional.of(transaction.jsonRpcRequest(signedTransactionHexString, transaction.getId()));
  }

  private boolean populateNonce() {
    try {
      transaction.updateNonce();
      return true;
    } catch (final RuntimeException e) {
      // It is currently recognised that the underlying nonce provider will wrap a transmission
      // exception in a Runtime exception.
      LOG.warn("Unable to get nonce from web3j provider.", e);
      this.handleFailure(e.getCause());
    } catch (final Throwable thrown) {
      LOG.debug("Failed to encode/serialize transaction: {}", transaction, thrown);
      context().fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
    }
    return false;
  }

  protected void sendTransaction(final String bodyContent) {
    final HttpServerRequest request = context().request();
    final MultiMap headersToSend = HeaderHelpers.createHeaders(request.headers());
    final VertxRequestTransmitter transmitter = transmitterFactory.create(this);
    transmitter.sendRequest(request.method(), headersToSend, request.path(), bodyContent);
  }
}

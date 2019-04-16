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

import tech.pegasys.ethsigner.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.jsonrpc.SendTransactionJsonParameters;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcError;
import tech.pegasys.ethsigner.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.requesthandler.JsonRpcErrorReporter;
import tech.pegasys.ethsigner.requesthandler.JsonRpcRequestHandler;
import tech.pegasys.ethsigner.signing.TransactionSigner;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class SendTransactionHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SendTransactionHandler.class);

  private final JsonRpcErrorReporter errorReporter;
  private final HttpClient ethNodeClient;
  private final TransactionSigner signer;
  private final RawTransactionConverter converter;
  private final NonceProvider nonceProvider;

  public SendTransactionHandler(
      final JsonRpcErrorReporter errorReporter,
      final HttpClient ethNodeClient,
      final TransactionSigner signer,
      final RawTransactionConverter converter,
      final NonceProvider nonceProvider) {
    this.errorReporter = errorReporter;
    this.ethNodeClient = ethNodeClient;
    this.signer = signer;
    this.converter = converter;
    this.nonceProvider = nonceProvider;
  }

  @Override
  public void handle(final HttpServerRequest httpServerRequest, final JsonRpcRequest request) {
    final SendTransactionJsonParameters params;
    try {
      params = SendTransactionJsonParameters.from(request);
    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      errorReporter.send(
          request,
          httpServerRequest,
          new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS));
      return;
    } catch (final IllegalArgumentException e) {
      LOG.debug("JSON Deserialisation failed for request: {}", request.getParams(), e);
      errorReporter.send(
          request,
          httpServerRequest,
          new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS));
      return;
    }

    if (senderNotUnlockedAccount(params)) {
      LOG.info(
          "From address ({}) does not match unlocked account ({})",
          params.sender(),
          signer.getAddress());
      errorReporter.send(
          request,
          httpServerRequest,
          new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS));
      return;
    }

    sendTransaction(params, httpServerRequest, request);
  }

  private void sendTransaction(
      final SendTransactionJsonParameters params,
      final HttpServerRequest httpServerRequest,
      final JsonRpcRequest request) {
    final RawTransaction rawTransaction = converter.from(params);

    final TransactionPassthrough sendTransactionTransmitter =
        createTransactionTransmitter(rawTransaction, httpServerRequest, request);

    sendTransactionTransmitter.send();
  }

  private TransactionPassthrough createTransactionTransmitter(
      final RawTransaction rawTransaction,
      final HttpServerRequest httpServerRequest,
      final JsonRpcRequest request) {
    final TransactionInformation tnxInfo;

    if (rawTransaction.getNonce() != null) {
      tnxInfo =
          new TransactionInformation(httpServerRequest, () -> rawTransaction, request.getId());
      return new TransactionPassthrough(ethNodeClient, tnxInfo, signer);
    } else {
      tnxInfo =
          new TransactionInformation(
              httpServerRequest,
              new NoncePopulatingRawTransactionSupplier(nonceProvider, rawTransaction),
              request.getId());
      return new RetryingTransactionTransmitter(ethNodeClient, tnxInfo, signer);
    }
  }

  private boolean senderNotUnlockedAccount(final SendTransactionJsonParameters params) {
    return !params.sender().equalsIgnoreCase(signer.getAddress());
  }
}

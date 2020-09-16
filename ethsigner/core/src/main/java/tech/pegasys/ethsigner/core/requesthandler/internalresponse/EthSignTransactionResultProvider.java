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
package tech.pegasys.ethsigner.core.requesthandler.internalresponse;

import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.ethsigner.core.AddressIndexedSignerProvider;
import tech.pegasys.ethsigner.core.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.ResultProvider;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.EthTransaction;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.ethsigner.core.signing.TransactionSerializer;
import tech.pegasys.signers.secp256k1.api.Signer;

import java.util.List;
import java.util.Optional;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EthSignTransactionResultProvider implements ResultProvider<String> {

  private static final Logger LOG = LogManager.getLogger();

  private final long chainId;
  private final AddressIndexedSignerProvider signerProvider;
  private final JsonDecoder decoder;

  public EthSignTransactionResultProvider(
      final long chainId,
      final AddressIndexedSignerProvider signerProvider,
      final JsonDecoder decoder) {
    this.chainId = chainId;
    this.signerProvider = signerProvider;
    this.decoder = decoder;
  }

  @Override
  public String createResponseResult(final JsonRpcRequest request) {
    LOG.debug("Transforming request {}, {}", request.getId(), request.getMethod());
    final Transaction transaction;
    try {
      transaction = createTransaction(request);

    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      throw new JsonRpcException(INVALID_PARAMS);
    } catch (final IllegalArgumentException | DecodeException e) {
      LOG.debug("JSON Deserialization failed for request: {}", request.getParams(), e);
      throw new JsonRpcException(INVALID_PARAMS);
    }

    if (!transaction.isNonceUserSpecified()) {
      LOG.debug("Nonce not present in request {}", request.getId());
      throw new JsonRpcException(INVALID_PARAMS);
    }

    LOG.debug("Obtaining signer for {}", transaction.sender());
    final Optional<Signer> Signer = signerProvider.getSigner(transaction.sender());
    if (Signer.isEmpty()) {
      LOG.info("From address ({}) does not match any available account", transaction.sender());
      throw new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
    }

    final TransactionSerializer transactionSerializer =
        new TransactionSerializer(Signer.get(), chainId);
    return transactionSerializer.serialize(transaction);
  }

  private Transaction createTransaction(final JsonRpcRequest request) {
    final EthSendTransactionJsonParameters params =
        fromRpcRequestToJsonParam(EthSendTransactionJsonParameters.class, request);
    return new EthTransaction(params, null, request.getId());
  }

  public <T> T fromRpcRequestToJsonParam(final Class<T> type, final JsonRpcRequest request) {
    final Object object;
    final Object params = request.getParams();
    if (params instanceof List) {
      @SuppressWarnings("unchecked")
      final List<Object> paramList = (List<Object>) params;
      if (paramList.size() != 1) {
        throw new IllegalArgumentException(
            type.getSimpleName()
                + " json Rpc requires one parameter, request contained "
                + paramList.size());
      }
      object = paramList.get(0);
    } else {
      object = params;
    }
    if (object == null) {
      throw new IllegalArgumentException(
          type.getSimpleName()
              + " json Rpc requires a valid parameter, request contained a null object");
    }
    final JsonObject receivedParams = JsonObject.mapFrom(object);
    return decoder.decodeValue(receivedParams.toBuffer(), type);
  }
}

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

import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcSuccessResponse;
import tech.pegasys.ethsigner.core.requesthandler.BodyProvider;
import tech.pegasys.ethsigner.core.requesthandler.JsonRpcBody;
import tech.pegasys.ethsigner.core.util.ByteUtils;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.TransactionSigner;
import tech.pegasys.signers.secp256k1.api.TransactionSignerProvider;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.web3j.utils.Numeric;

public class EthSignBodyProvider implements BodyProvider {

  private static final Logger LOG = LogManager.getLogger();

  private final TransactionSignerProvider transactionSignerProvider;

  public EthSignBodyProvider(final TransactionSignerProvider transactionSignerProvider) {
    this.transactionSignerProvider = transactionSignerProvider;
  }

  @Override
  public JsonRpcBody getBody(final JsonRpcRequest request) {
    try {
      @SuppressWarnings("unchecked")
      final List<String> params = (List<String>) request.getParams();
      if (params == null || params.size() != 2) {
        LOG.info(
            "eth_sign should have a list of 2 parameters, but has {}",
            params == null ? "null" : params.size());
        return new JsonRpcBody(INVALID_PARAMS);
      }
      final String address = params.get(0);
      final Optional<TransactionSigner> transactionSigner =
          transactionSignerProvider.getSigner(address);
      if (transactionSigner.isEmpty()) {
        LOG.info("Address ({}) does not match any available account", address);
        return new JsonRpcBody(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
      }
      final TransactionSigner signer = transactionSigner.get();
      final String originalMessage = params.get(1);
      final String message =
          (char) 25 + "Ethereum Signed Message:\n" + originalMessage.length() + originalMessage;
      final Signature signature = signer.sign(message.getBytes(StandardCharsets.UTF_8));

      final Bytes outputSignature =
          Bytes.concatenate(
              Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getR()))),
              Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getS()))),
              Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getV())));
      final JsonRpcSuccessResponse response =
          new JsonRpcSuccessResponse(
              request.getId(), Numeric.toHexString(outputSignature.toArray()));
      return new JsonRpcBody(Json.encodeToBuffer(response));
    } catch (final ClassCastException e) {
      LOG.info(
          "eth_sign should have a list of 2 parameters, but received an object: {}",
          request.getParams());
      return new JsonRpcBody(INVALID_PARAMS);
    } catch (final Exception e) {
      LOG.info("Unexpected error", e);
      return new JsonRpcBody(INTERNAL_ERROR);
    }
  }
}

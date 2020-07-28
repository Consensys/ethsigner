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
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.ResultProvider;
import tech.pegasys.ethsigner.core.util.ByteUtils;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.Signer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.web3j.utils.Numeric;

public class EthSignResultProvider implements ResultProvider<String> {

  private static final Logger LOG = LogManager.getLogger();

  private final AddressIndexedSignerProvider transactionSignerProvider;

  public EthSignResultProvider(final AddressIndexedSignerProvider transactionSignerProvider) {
    this.transactionSignerProvider = transactionSignerProvider;
  }

  @Override
  public String createResponseResult(final JsonRpcRequest request) {
    final List<String> params = getParams(request);
    if (params == null || params.size() != 2) {
      LOG.info(
          "eth_sign should have a list of 2 parameters, but has {}",
          params == null ? "null" : params.size());
      throw new JsonRpcException(INVALID_PARAMS);
    }

    final String address = params.get(0);
    final Optional<Signer> transactionSigner = transactionSignerProvider.getSigner(address);
    if (transactionSigner.isEmpty()) {
      LOG.info("Address ({}) does not match any available account", address);
      throw new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
    }

    final Signer signer = transactionSigner.get();
    final String originalMessage = params.get(1);
    final String message =
        (char) 25 + "Ethereum Signed Message:\n" + originalMessage.length() + originalMessage;
    final Signature signature = signer.sign(message.getBytes(StandardCharsets.UTF_8));

    final Bytes outputSignature =
        Bytes.concatenate(
            Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getR()))),
            Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getS()))),
            Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getV())));
    return Numeric.toHexString(outputSignature.toArray());
  }

  private List<String> getParams(final JsonRpcRequest request) {
    try {
      @SuppressWarnings("unchecked")
      final List<String> params = (List<String>) request.getParams();
      return params;
    } catch (final ClassCastException e) {
      LOG.info(
          "eth_sign should have a list of 2 parameters, but received an object: {}",
          request.getParams());
      throw new JsonRpcException(INVALID_PARAMS);
    }
  }
}

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
package tech.pegasys.ethfirewall.signing;

import tech.pegasys.ethfirewall.jsonrpc.SendTransactionJsonParameters;
import tech.pegasys.ethfirewall.jsonrpc.SendTransactionJsonRpcRequest;
import tech.pegasys.ethfirewall.signing.web3j.TransactionEncoder;

import java.math.BigInteger;
import java.util.Optional;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.utils.Numeric;

public class TransactionSigner {

  private static final int HEXADECIMAL = 16;
  private static final int HEXADECIMAL_PREFIX_LENGTH = 2;

  private final Credentials credentials;
  private final ChainIdProvider chain;

  public TransactionSigner(final ChainIdProvider chain, final Credentials credentials) {
    this.chain = chain;
    this.credentials = credentials;
  }

  public String signTransaction(final SendTransactionJsonRpcRequest request) {
    final RawTransaction rawTransaction = rawTransaction(request);

    // Sign the transaction using the post Spurious Dragon technique
    final byte[] signedMessage =
        TransactionEncoder.signMessage(rawTransaction, chain.id(), credentials);
    return Numeric.toHexString(signedMessage);
  }

  private RawTransaction rawTransaction(final SendTransactionJsonRpcRequest request) {
    final SendTransactionJsonParameters params = request.getParams();

    return RawTransaction.createTransaction(
        nonce(params),
        optionalHex(params.gasPrice()),
        gas(params),
        params.receiver(),
        optionalHex(params.value()),
        params.data());
  }

  private BigInteger nonce(final SendTransactionJsonParameters params) {
    if (params.nonce().isPresent()) {

      return optionalHex(params.nonce());
    } else {
      // TODO when missing nonce - get it from somewhere
      return BigInteger.ZERO;
    }
  }

  private BigInteger gas(final SendTransactionJsonParameters params) {

    if (params.gas().isPresent()) {
      return hex(params.gas().get().substring(HEXADECIMAL_PREFIX_LENGTH));
    }

    // TODO(tmm): This should be configurable, but currently matches Geth.
    return new BigInteger("90000");
  }

  private BigInteger hex(final String value) {
    return new BigInteger(value, HEXADECIMAL);
  }

  // TODO validate hex format - prefix 0x
  private BigInteger optionalHex(final Optional<String> value) {
    return value.isPresent() ? hex(value.get().substring(HEXADECIMAL_PREFIX_LENGTH)) : null;
  }
}

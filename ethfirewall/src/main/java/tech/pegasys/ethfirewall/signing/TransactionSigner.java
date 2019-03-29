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

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.utils.Numeric;

public class TransactionSigner {

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
        params.gasPrice().orElse(null),
        gas(params),
        receiver(params),
        params.value().orElse(null),
        params.data());
  }

  private String receiver(final SendTransactionJsonParameters params) {
    return params.receiver().isPresent() ? params.receiver().get().toString() : null;
  }

  private BigInteger nonce(final SendTransactionJsonParameters params) {
    if (params.nonce().isPresent()) {
      return params.nonce().get();
    } else {
      // TODO when missing nonce - sensible retrieval (PIE-1468)
      throw new IllegalArgumentException("Missing nonce");
    }
  }

  private BigInteger gas(final SendTransactionJsonParameters params) {

    if (params.gas().isPresent()) {
      return params.gas().get();
    }

    // TODO This should be configurable, but currently matches Geth.
    return new BigInteger("90000");
  }
}

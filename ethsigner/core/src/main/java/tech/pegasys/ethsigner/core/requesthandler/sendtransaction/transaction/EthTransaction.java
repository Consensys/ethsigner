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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import tech.pegasys.ethsigner.core.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

import java.math.BigInteger;
import java.util.List;

import com.google.common.base.MoreObjects;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

public class EthTransaction implements Transaction {
  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";
  private final EthSendTransactionJsonParameters ethSendTransactionJsonParameters;
  private final RawTransactionBuilder rawTransactionBuilder;
  private final NonceProvider nonceProvider;
  private final JsonRpcRequestId id;

  EthTransaction(
      final EthSendTransactionJsonParameters ethSendTransactionJsonParameters,
      final NonceProvider nonceProvider,
      final JsonRpcRequestId id) {
    this.ethSendTransactionJsonParameters = ethSendTransactionJsonParameters;
    this.rawTransactionBuilder = RawTransactionBuilder.from(ethSendTransactionJsonParameters);
    this.nonceProvider = nonceProvider;
    this.id = id;
  }

  @Override
  public void updateNonce() {
    final BigInteger nonce = nonceProvider.getNonce();
    rawTransactionBuilder.withNonce(nonce);
  }

  @Override
  public byte[] rlpEncode(final SignatureData signatureData) {
    final RawTransaction rawTransaction = rawTransactionBuilder.build();
    final List<RlpType> values = TransactionEncoder.asRlpValues(rawTransaction, signatureData);
    final RlpList rlpList = new RlpList(values);
    return RlpEncoder.encode(rlpList);
  }

  @Override
  public boolean isNonceUserSpecified() {
    return ethSendTransactionJsonParameters.nonce().isPresent();
  }

  @Override
  public String sender() {
    return ethSendTransactionJsonParameters.sender();
  }

  @Override
  public JsonRpcRequest jsonRpcRequest(
      final String signedTransactionHexString, final JsonRpcRequestId id) {
    return Transaction.jsonRpcRequest(signedTransactionHexString, id, JSON_RPC_METHOD);
  }

  @Override
  public JsonRpcRequestId getId() {
    return id;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("ethSendTransactionJsonParameters", ethSendTransactionJsonParameters)
        .add("rawTransactionBuilder", rawTransactionBuilder)
        .add("id", id)
        .toString();
  }
}

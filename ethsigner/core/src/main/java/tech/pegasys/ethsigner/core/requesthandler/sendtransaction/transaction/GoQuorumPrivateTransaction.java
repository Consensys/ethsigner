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

import tech.pegasys.ethsigner.core.jsonrpc.EeaSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.EnclaveLookupIdProvider;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

import java.util.List;

import com.google.common.base.MoreObjects;
import org.web3j.protocol.eea.crypto.RawPrivateTransaction;
import org.web3j.utils.Base64String;
import org.web3j.utils.Restriction;

public class GoQuorumPrivateTransaction extends PrivateTransaction {

  private final List<Base64String> privateFor;
  private final EnclaveLookupIdProvider enclaveLookupIdProvider;
  private String lookupId;

  public static GoQuorumPrivateTransaction from(
      final EeaSendTransactionJsonParameters transactionJsonParameters,
      final NonceProvider nonceProvider,
      final EnclaveLookupIdProvider enclaveLookupIdProvider,
      final JsonRpcRequestId id) {

    if (transactionJsonParameters.privateFor().isEmpty()) {
      throw new IllegalArgumentException("Transaction does not contain a valid privateFor list.");
    }

    return new GoQuorumPrivateTransaction(
        transactionJsonParameters,
        nonceProvider,
        enclaveLookupIdProvider,
        id,
        transactionJsonParameters.privateFor().get());
  }

  private GoQuorumPrivateTransaction(
      final EeaSendTransactionJsonParameters transactionJsonParameters,
      final NonceProvider nonceProvider,
      final EnclaveLookupIdProvider enclaveLookupIdProvider,
      final JsonRpcRequestId id,
      final List<Base64String> privateFor) {
    super(transactionJsonParameters, nonceProvider, id);
    this.privateFor = privateFor;
    this.enclaveLookupIdProvider = enclaveLookupIdProvider;
  }

  @Override
  public String getJsonRpcMethodName() {
    return "goquorum_storeRaw";
  }

  @Override
  public void updateNonce() {
    this.nonce = nonceProvider.getNonce();
    this.lookupId = enclaveLookupIdProvider.getLookupId();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("transactionJsonParameters", transactionJsonParameters)
        .add("nonceProvider", nonceProvider)
        .add("id", id)
        .add("nonce", nonce)
        .add("enclaveLookupIdProvider", enclaveLookupIdProvider)
        .add("lookupId", lookupId)
        .toString();
  }

  @Override
  protected RawPrivateTransaction createTransaction() {
    return RawPrivateTransaction.createTransaction(
        nonce,
        transactionJsonParameters.gasPrice().orElse(DEFAULT_GAS_PRICE),
        transactionJsonParameters.gas().orElse(DEFAULT_GAS),
        transactionJsonParameters.receiver().orElse(DEFAULT_TO),
        transactionJsonParameters.data().orElse(DEFAULT_DATA),
        transactionJsonParameters.privateFrom(),
        privateFor,
        Restriction.fromString(transactionJsonParameters.restriction()));
  }
}

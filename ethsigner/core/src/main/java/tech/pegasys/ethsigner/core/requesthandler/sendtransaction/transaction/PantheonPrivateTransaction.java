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
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

import org.web3j.protocol.eea.crypto.RawPrivateTransaction;
import org.web3j.utils.Base64String;
import org.web3j.utils.Restriction;

public class PantheonPrivateTransaction extends PrivateTransaction {

  public static PantheonPrivateTransaction from(
      final EeaSendTransactionJsonParameters transactionJsonParameters,
      final NonceProvider nonceProvider,
      final JsonRpcRequestId id) {
    if (!transactionJsonParameters.privacyGroupId().isPresent()) {
      throw new RuntimeException("Transaction does not contain a valid privacyGroup.");
    }

    return new PantheonPrivateTransaction(
        transactionJsonParameters,
        nonceProvider,
        id,
        transactionJsonParameters.privacyGroupId().get());
  }

  private final PrivacyIdentifier privacyGroupId;

  private PantheonPrivateTransaction(
      EeaSendTransactionJsonParameters transactionJsonParameters,
      NonceProvider nonceProvider,
      JsonRpcRequestId id,
      PrivacyIdentifier privacyGroupId) {
    super(transactionJsonParameters, nonceProvider, id);
    this.privacyGroupId = privacyGroupId;
  }

  @Override
  protected RawPrivateTransaction createTransaction() {
    return RawPrivateTransaction.createTransaction(
        nonce,
        transactionJsonParameters.gasPrice().orElse(DEFAULT_GAS_PRICE),
        transactionJsonParameters.gas().orElse(DEFAULT_GAS),
        transactionJsonParameters.receiver().orElse(DEFAULT_TO),
        transactionJsonParameters.data().orElse(DEFAULT_DATA),
        Base64String.wrap(transactionJsonParameters.privateFrom().getRaw()),
        Base64String.wrap(privacyGroupId.getRaw()),
        Restriction.fromString(transactionJsonParameters.restriction()));
  }
}

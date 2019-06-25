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

import static tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.EeaUtils.generatePrivacyGroupId;

import tech.pegasys.ethsigner.core.jsonrpc.EeaSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.eea.Eea;

public class TransactionFactory {

  private final Eea eea;
  private final Web3j web3j;

  public TransactionFactory(final Eea eea, final Web3j web3j) {
    this.eea = eea;
    this.web3j = web3j;
  }

  public Transaction createTransaction(final JsonRpcRequest request) {
    final String method = request.getMethod().toLowerCase();
    switch (method) {
      case "eth_sendtransaction":
        return createEthTransaction(request);
      case "eea_sendtransaction":
        return createEeaTransaction(request);
      default:
        throw new IllegalStateException("Unknown send transaction method " + method);
    }
  }

  private Transaction createEeaTransaction(final JsonRpcRequest request) {
    final EeaSendTransactionJsonParameters params = EeaSendTransactionJsonParameters.from(request);
    final String privacyGroupId = generatePrivacyGroupId(params.privateFrom(), params.privateFor());
    final NonceProvider nonceProvider =
        new EeaWeb3jNonceProvider(eea, params.sender(), privacyGroupId);
    return new EeaTransaction(params, nonceProvider, request.getId());
  }

  private Transaction createEthTransaction(final JsonRpcRequest request) {
    final EthSendTransactionJsonParameters params = EthSendTransactionJsonParameters.from(request);
    final NonceProvider ethNonceProvider = new EthWeb3jNonceProvider(web3j, params.sender());
    return new EthTransaction(params, ethNonceProvider, request.getId());
  }
}

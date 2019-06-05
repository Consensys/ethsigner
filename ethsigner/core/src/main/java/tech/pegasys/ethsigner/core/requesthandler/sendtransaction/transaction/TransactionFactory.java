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
import tech.pegasys.ethsigner.core.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;

public class TransactionFactory {

  public Transaction createTransaction(final JsonRpcRequest request) {
    final String method = request.getMethod().toLowerCase();
    switch (method) {
      case "eth_sendtransaction":
        return new EthTransaction(EthSendTransactionJsonParameters.from(request), request.getId());
      case "eea_sendtransaction":
        return new EeaTransaction(EeaSendTransactionJsonParameters.from(request), request.getId());
      default:
        throw new IllegalStateException("Unknown send transaction method " + method);
    }
  }
}

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
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.pantheon.Pantheon;

public class TransactionFactory {

  private static final Logger LOG = LogManager.getLogger();

  private final Pantheon pantheon;
  private final Web3j web3j;

  // TODO(tmm): Remove this once eea_GetTransaction is available from eea namespace in web3j
  private final Web3jService web3jService;

  public TransactionFactory(
      final Pantheon pantheon, final Web3j web3j, final Web3jService web3jService) {
    this.pantheon = pantheon;
    this.web3j = web3j;
    this.web3jService = web3jService;
  }

  public static TransactionFactory createFrom(final Web3jService web3jService) {
    final Web3j web3j = new JsonRpc2_0Web3j(web3jService);
    final Pantheon pantheon = Pantheon.build(web3jService);

    return new TransactionFactory(pantheon, web3j, web3jService);
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

  private Transaction createEthTransaction(final JsonRpcRequest request) {
    final EthSendTransactionJsonParameters params = EthSendTransactionJsonParameters.from(request);
    final NonceProvider ethNonceProvider = new EthWeb3jNonceProvider(web3j, params.sender());
    return new EthTransaction(params, ethNonceProvider, request.getId());
  }

  private Transaction createEeaTransaction(final JsonRpcRequest request) {
    final EeaSendTransactionJsonParameters params = EeaSendTransactionJsonParameters.from(request);

    if (params.privacyGroupId().isPresent() == params.privateFor().isPresent()) {
      LOG.warn(
          "Illegal private transaction received; privacyGroup (present = {}) and privateFor (present = {}) are mutually exclusive.",
          params.privacyGroupId().isPresent(),
          params.privateFor().isPresent());
      throw new RuntimeException("PrivacyGroup and PrivateFor are mutually exclusive.");
    }

    if (params.privacyGroupId().isPresent()) {
      final NonceProvider nonceProvider =
          new PantheonPrivateNonceProvider(
              pantheon, params.sender(), params.privacyGroupId().get());
      return PantheonPrivateTransaction.from(params, nonceProvider, request.getId());
    }

    final NonceProvider nonceProvider =
        new EeaPrivateNonceProvider(
            web3jService, params.sender(), params.privateFrom(), params.privateFor().get());
    return EeaPrivateTransaction.from(params, nonceProvider, request.getId());
  }
}

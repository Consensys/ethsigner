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

import static tech.pegasys.ethsigner.core.jsonrpc.RpcUtil.JSON_RPC_VERSION;

import tech.pegasys.ethsigner.core.jsonrpc.EeaSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.EnclaveLookupIdProvider;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

import java.util.List;

import com.google.common.base.MoreObjects;
import io.vertx.core.json.JsonObject;
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
    return "eth_sendRawPrivateTransaction";
  }

  @Override
  public void updateNonce() {
    this.nonce = nonceProvider.getNonce();
    // TODO refactor or at least rename this method - it now does TWO things
    // TODO data is optional - do we also accept _input_ as per
    // https://docs.goquorum.consensys.net/en/stable/Reference/APIs/PrivacyAPI/#ethsendtransaction
    final String payload = this.transactionJsonParameters.data().get();
    this.lookupId = enclaveLookupIdProvider.getLookupId(payload);
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

  private JsonObject getGoQuorumRawTxJsonParams() {
    final JsonObject jsonObject = new JsonObject();
    jsonObject.put("privateFrom", transactionJsonParameters.privateFrom().toString());
    jsonObject.put("privateFor", Base64String.unwrapList(privateFor));
    jsonObject.put("privacyFlag", 0);
    return jsonObject;
  }

  @Override
  public JsonRpcRequest jsonRpcRequest(final String payload, final JsonRpcRequestId id) {
    final JsonRpcRequest request = new JsonRpcRequest(JSON_RPC_VERSION, getJsonRpcMethodName());
    request.setParams(new Object[] {payload, getGoQuorumRawTxJsonParams()});
    request.setId(id);
    return request;
  }

  @Override
  protected RawPrivateTransaction createTransaction() {
    return RawPrivateTransaction.createTransaction(
        nonce,
        transactionJsonParameters.gasPrice().orElse(DEFAULT_GAS_PRICE),
        transactionJsonParameters.gas().orElse(DEFAULT_GAS),
        transactionJsonParameters.receiver().orElse(DEFAULT_TO),
        lookupId,
        transactionJsonParameters.privateFrom(),
        privateFor,
        Restriction.fromString(transactionJsonParameters.restriction()));
  }
}

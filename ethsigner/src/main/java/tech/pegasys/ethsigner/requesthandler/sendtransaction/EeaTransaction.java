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
package tech.pegasys.ethsigner.requesthandler.sendtransaction;

import tech.pegasys.ethsigner.jsonrpc.EeaSendTransactionJsonParameters;
import tech.pegasys.ethsigner.jsonrpc.EthSendTransactionJsonParameters;

import java.math.BigInteger;
import java.util.List;

import com.google.common.base.MoreObjects;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.protocol.eea.crypto.PrivateTransactionEncoder;
import org.web3j.protocol.eea.crypto.RawPrivateTransaction;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

public class EeaTransaction implements Transaction {
  private final EthSendTransactionJsonParameters eeaSendTransactionJsonParameters;
  private final RawPrivateTransactionBuilder rawPrivateTransactionBuilder;

  public EeaTransaction(final EeaSendTransactionJsonParameters eeaSendTransactionJsonParameters) {
    this.eeaSendTransactionJsonParameters = eeaSendTransactionJsonParameters;
    this.rawPrivateTransactionBuilder =
        RawPrivateTransactionBuilder.from(eeaSendTransactionJsonParameters);
  }

  @Override
  public void updateNonce(final BigInteger nonce) {
    rawPrivateTransactionBuilder.updateNonce(nonce);
  }

  @Override
  public byte[] rlpEncode(final SignatureData signatureData) {
    final RawPrivateTransaction rawTransaction = rawPrivateTransactionBuilder.build();
    final List<RlpType> values =
        PrivateTransactionEncoder.asRlpValues(rawTransaction, signatureData);
    final RlpList rlpList = new RlpList(values);
    return RlpEncoder.encode(rlpList);
  }

  @Override
  public boolean hasNonce() {
    return eeaSendTransactionJsonParameters.nonce().isPresent();
  }

  @Override
  public String sender() {
    return eeaSendTransactionJsonParameters.sender();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("eeaSendTransactionJsonParameters", eeaSendTransactionJsonParameters)
        .add("rawPrivateTransactionBuilder", rawPrivateTransactionBuilder)
        .toString();
  }
}

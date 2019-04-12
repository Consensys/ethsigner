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
package tech.pegasys.ethsigner.signing;

import tech.pegasys.ethsigner.signing.web3j.TransactionEncoder;

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

  public String getAddress() {
    return credentials.getAddress();
  }

  public String signTransaction(final RawTransaction rawTransaction) {
    // Sign the transaction using the post Spurious Dragon technique
    final byte[] signedMessage =
        TransactionEncoder.signMessage(rawTransaction, chain.id(), credentials);
    return Numeric.toHexString(signedMessage);
  }
}

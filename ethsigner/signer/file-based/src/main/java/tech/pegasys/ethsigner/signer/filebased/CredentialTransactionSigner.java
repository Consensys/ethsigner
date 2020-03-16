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
package tech.pegasys.ethsigner.signer.filebased;

import tech.pegasys.ethsigner.core.signing.Signature;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.math.BigInteger;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;

public class CredentialTransactionSigner implements TransactionSigner {

  private final Credentials credentials;

  public CredentialTransactionSigner(final Credentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public Signature sign(final byte[] data) {
    final SignatureData signature = Sign.signMessage(data, credentials.getEcKeyPair());
    return new Signature(
        new BigInteger(signature.getV()),
        new BigInteger(1, signature.getR()),
        new BigInteger(1, signature.getS()));
  }

  @Override
  public String getAddress() {
    return credentials.getAddress();
  }
}

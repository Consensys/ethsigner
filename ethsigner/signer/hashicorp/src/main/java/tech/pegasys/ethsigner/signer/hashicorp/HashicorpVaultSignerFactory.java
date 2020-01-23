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
package tech.pegasys.ethsigner.signer.hashicorp;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.signer.filebased.CredentialTransactionSigner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;

public class HashicorpVaultSignerFactory {
  private static final Logger LOG = LogManager.getLogger();

  public static TransactionSigner createSigner(final HashicorpConfig hashicorpConfig) {
    final Credentials credentials =
        Credentials.create(new HashicorpClient(hashicorpConfig).requestSecretFromVault());
    if (credentials.getAddress() != null) {
      LOG.debug("Successfully retrieved the credentials from the Hashicorp vault.");
    }
    return new CredentialTransactionSigner(credentials);
  }
}

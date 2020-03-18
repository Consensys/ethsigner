/*
 * Copyright 2020 ConsenSys AG.
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

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.signer.filebased.CredentialTransactionSigner;
import tech.pegasys.signers.hashicorp.HashicorpConnection;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.hashicorp.HashicorpException;
import tech.pegasys.signers.hashicorp.config.HashicorpKeyConfig;

import io.vertx.core.Vertx;
import org.web3j.crypto.Credentials;

public class HashicorpSignerFactory {

  private Vertx vertx;

  public HashicorpSignerFactory(final Vertx vertx) {
    this.vertx = vertx;
  }

  public TransactionSigner create(final HashicorpKeyConfig keyConfig) {
    try {
      final HashicorpConnectionFactory connectionFactory = new HashicorpConnectionFactory(vertx);
      final HashicorpConnection connection =
          connectionFactory.create(keyConfig.getConnectionParams());
      final String secret = connection.fetchKey(keyConfig.getKeyDefinition());
      final Credentials credentials = Credentials.create(secret);
      return new CredentialTransactionSigner(credentials);
    } catch (final HashicorpException e) {
      throw new TransactionSignerInitializationException(
          "Failed to extract secret from Hashicorp vault.", e);
    }
  }

  public void shutdown() {
    if (vertx != null) {
      vertx.close();
      vertx = null;
    }
  }
}

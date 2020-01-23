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

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.signer.filebased.CredentialTransactionSigner;

import java.util.Map;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;

public class HashicorpVaultSignerFactory {
  private static final String VAULT_KV_MAP_KEY = "value";
  private static final String KEY_MISSING_MSG =
      "Required key [%s] doesn't exist in Hashicorp Vault Response";

  public static TransactionSigner createSigner(final HashicorpConfig hashicorpConfig) {
    final JsonObject jsonResponse = new HashicorpVaultKVEngineClient(hashicorpConfig).requestData();
    final Map<String, String> dataMap = HashicorpKVResponseMapper.extractMapFromJson(jsonResponse);
    if (!dataMap.containsKey(VAULT_KV_MAP_KEY)) {
      throw new TransactionSignerInitializationException(
          String.format(KEY_MISSING_MSG, VAULT_KV_MAP_KEY));
    }
    final String privateKey = dataMap.get(VAULT_KV_MAP_KEY);
    final Credentials credentials = Credentials.create(privateKey);
    return new CredentialTransactionSigner(credentials);
  }
}

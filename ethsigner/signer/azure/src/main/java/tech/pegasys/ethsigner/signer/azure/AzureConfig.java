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
package tech.pegasys.ethsigner.signer.azure;

public class AzureConfig {
  private final String keyVaultName;
  private final String keyName;
  private final String keyVersion;
  private final String clientId;
  private final String clientSecret;

  public AzureConfig(
      final String keyVaultName,
      final String keyName,
      final String keyVersion,
      final String clientId,
      final String clientSecret) {
    this.keyVaultName = keyVaultName;
    this.keyName = keyName;
    this.keyVersion = keyVersion;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public String getKeyvaultName() {
    return keyVaultName;
  }

  public String getKeyName() {
    return keyName;
  }

  public String getKeyVersion() {
    return keyVersion;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }
}

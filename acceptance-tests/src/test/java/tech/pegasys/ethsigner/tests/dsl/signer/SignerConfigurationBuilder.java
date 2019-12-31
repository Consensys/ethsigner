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
package tech.pegasys.ethsigner.tests.dsl.signer;

import tech.pegasys.ethsigner.core.TlsOptions;
import tech.pegasys.ethsigner.tests.dsl.utils.HashicorpVault;

import java.nio.file.Path;
import java.util.Optional;

public class SignerConfigurationBuilder {

  /** ChainId defined in the dev mode genesis. */
  private static final String CHAIN_ID = "2018";

  private static final String LOCALHOST = "127.0.0.1";

  private int httpRpcPort;
  private int webSocketPort;
  private String keyVaultName;
  private Path multiKeySignerDirectory;
  private HashicorpVault hashicorpVault;
  private TlsOptions serverTlsOptions;

  public SignerConfigurationBuilder withHttpRpcPort(final int port) {
    httpRpcPort = port;
    return this;
  }

  public SignerConfigurationBuilder withWebSocketPort(final int port) {
    webSocketPort = port;
    return this;
  }

  public SignerConfigurationBuilder withHashicorpSigner(final HashicorpVault hashicorpVault) {
    this.hashicorpVault = hashicorpVault;
    return this;
  }

  public SignerConfigurationBuilder withAzureKeyVault(final String keyVaultName) {
    this.keyVaultName = keyVaultName;
    return this;
  }

  public SignerConfigurationBuilder withMultiKeySignerDirectory(
      final Path multiKeySignerDirectory) {
    this.multiKeySignerDirectory = multiKeySignerDirectory;
    return this;
  }

  public SignerConfigurationBuilder withServerTlsOptions(final TlsOptions serverTlsOptions) {
    this.serverTlsOptions = serverTlsOptions;
    return this;
  }

  public SignerConfiguration build() {
    final TransactionSignerParamsSupplier transactionSignerParamsSupplier =
        new TransactionSignerParamsSupplier(hashicorpVault, keyVaultName, multiKeySignerDirectory);
    return new SignerConfiguration(
        CHAIN_ID,
        LOCALHOST,
        httpRpcPort,
        webSocketPort,
        transactionSignerParamsSupplier,
        Optional.ofNullable(serverTlsOptions));
  }
}

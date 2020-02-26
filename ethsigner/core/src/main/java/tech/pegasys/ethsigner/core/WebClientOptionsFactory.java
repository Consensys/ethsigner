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
package tech.pegasys.ethsigner.core;

import static org.apache.tuweni.net.tls.VertxTrustOptions.whitelistServers;

import tech.pegasys.ethsigner.core.config.Config;
import tech.pegasys.ethsigner.core.config.KeyStoreOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;

class WebClientOptionsFactory {
  public WebClientOptions createWebClientOptions(final Config config) {
    final WebClientOptions clientOptions =
        new WebClientOptions()
            .setDefaultPort(config.getDownstreamHttpPort())
            .setDefaultHost(config.getDownstreamHttpHost());

    applyTlsOptions(clientOptions, config);
    return clientOptions;
  }

  private void applyTlsOptions(final WebClientOptions webClientOptions, final Config config) {
    final Optional<ClientTlsOptions> optionalClientTlsOptions = config.getClientTlsOptions();
    if (optionalClientTlsOptions.isEmpty()) {
      return;
    }

    webClientOptions.setSsl(true);

    final ClientTlsOptions clientTlsOptions = optionalClientTlsOptions.get();

    applyTrustOptions(
        webClientOptions,
        clientTlsOptions.getKnownServersFile(),
        clientTlsOptions.isCaAuthEnabled());
    applyKeyStoreOptions(webClientOptions, clientTlsOptions.getKeyStoreOptions());
  }

  private void applyTrustOptions(
      final WebClientOptions webClientOptions,
      final Optional<Path> knownServerFile,
      final boolean caAuthEnabled) {

    if (knownServerFile.isPresent()) {
      try {
        webClientOptions.setTrustOptions(whitelistServers(knownServerFile.get(), caAuthEnabled));
      } catch (RuntimeException e) {
        throw new InitializationException("Failed to load known server file.", e);
      }
    }

    if (knownServerFile.isEmpty() && !caAuthEnabled) {
      throw new InitializationException(
          "Must specify a known-server file if CA-signed option is disabled");
    }
    // otherwise knownServerFile is empty and caAuthEnabled is true which is the default situation
  }

  private void applyKeyStoreOptions(
      final WebClientOptions webClientOptions,
      final Optional<KeyStoreOptions> optionalKeyStoreOptions) {

    if (optionalKeyStoreOptions.isEmpty()) {
      return;
    }

    try {
      webClientOptions.setPfxKeyCertOptions(convertFrom(optionalKeyStoreOptions.get()));
    } catch (final IOException e) {
      throw new InitializationException("Failed to load client certificate keystore.", e);
    }
  }

  private PfxOptions convertFrom(final KeyStoreOptions keyStoreOptions) throws IOException {
    final String password = FileUtil.readFirstLineFromFile(keyStoreOptions.getPasswordFile());
    return new PfxOptions()
        .setPassword(password)
        .setPath(keyStoreOptions.getKeyStoreFile().toString());
  }
}

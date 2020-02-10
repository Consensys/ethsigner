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
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsCertificateOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsTrustOptions;
import tech.pegasys.ethsigner.core.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;

class WebClientOptionsBuilder {
  public WebClientOptions build(final Config config) {
    final WebClientOptions clientOptions =
        new WebClientOptions()
            .setDefaultPort(config.getDownstreamHttpPort())
            .setDefaultHost(config.getDownstreamHttpHost());

    applyTlsOptions(clientOptions, config);
    return clientOptions;
  }

  private void applyTlsOptions(final WebClientOptions webClientOptions, final Config config) {
    final Optional<ClientTlsOptions> optionalClientTlsOptions = config.getClientTlsOptions();
    if (optionalClientTlsOptions.isEmpty() || !optionalClientTlsOptions.get().isTlsEnabled()) {
      return;
    }

    webClientOptions.setSsl(true);

    final ClientTlsOptions clientTlsOptions = optionalClientTlsOptions.get();

    applyClientTlsTrustOptions(webClientOptions, clientTlsOptions.getClientTlsTrustOptions());
    applyClientTlsCertificateOptions(
        webClientOptions, clientTlsOptions.getClientTlsCertificateOptions());
  }

  private void applyClientTlsTrustOptions(
      final WebClientOptions webClientOptions,
      final Optional<ClientTlsTrustOptions> optionalClientTlsTrustOptions) {

    if (optionalClientTlsTrustOptions.isEmpty()) {
      return; // CA trust is enabled by default.
    }

    final Optional<Path> optionalKnownServerFile =
        optionalClientTlsTrustOptions.get().getKnownServerFile();
    final boolean allowCATrust =
        optionalClientTlsTrustOptions.get().isCaSignedServerCertificateAllowed();

    if (optionalKnownServerFile.isEmpty() && !allowCATrust) {
      throw new InitializationException(
          "Must specify a known-server file if CA-signed option is disabled");
    }

    try {
      webClientOptions.setTrustOptions(
          whitelistServers(optionalKnownServerFile.get(), allowCATrust));
    } catch (RuntimeException e) {
      throw new InitializationException("Failed to load known server file.", e);
    }
  }

  private void applyClientTlsCertificateOptions(
      final WebClientOptions webClientOptions,
      final Optional<ClientTlsCertificateOptions> optionalClientTlsCertificateOptions) {

    if (optionalClientTlsCertificateOptions.isEmpty()) {
      return;
    }

    try {
      webClientOptions.setPfxKeyCertOptions(convertFrom(optionalClientTlsCertificateOptions.get()));
    } catch (final IOException e) {
      throw new InitializationException("Failed to load client certificate keystore.", e);
    }
  }

  private PfxOptions convertFrom(final ClientTlsCertificateOptions clientTlsCertificateOptions)
      throws IOException {
    final String password =
        FileUtil.readFirstLineFromFile(clientTlsCertificateOptions.getKeyStorePasswordFile());
    return new PfxOptions()
        .setPassword(password)
        .setPath(clientTlsCertificateOptions.getKeyStoreFile().toString());
  }
}

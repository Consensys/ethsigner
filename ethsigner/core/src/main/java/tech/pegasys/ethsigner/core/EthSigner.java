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
package tech.pegasys.ethsigner.core;

import static org.apache.tuweni.net.tls.VertxTrustOptions.whitelistServers;

import tech.pegasys.ethsigner.core.config.ClientAuthConstraints;
import tech.pegasys.ethsigner.core.config.Config;
import tech.pegasys.ethsigner.core.config.TlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsCertificateOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsTrustOptions;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.net.tls.VertxTrustOptions;

public final class EthSigner {

  private static final Logger LOG = LogManager.getLogger();

  private final Config config;
  private final TransactionSignerProvider transactionSignerProvider;

  public EthSigner(final Config config, final TransactionSignerProvider transactionSignerProvider) {
    this.config = config;
    this.transactionSignerProvider = transactionSignerProvider;
  }

  public void run() {

    final Duration downstreamHttpRequestTimeout = config.getDownstreamHttpRequestTimeout();
    if (downstreamHttpRequestTimeout.toMillis() <= 0) {
      LOG.error("Http request timeout must be greater than 0.");
      return;
    }

    if (config.getHttpListenHost().equals(config.getDownstreamHttpHost())
        && config.getHttpListenPort().equals(config.getDownstreamHttpPort())) {
      LOG.error("Http host and port must be different to the downstream host and port.");
      return;
    }

    final JsonDecoder jsonDecoder = createJsonDecoder();

    final WebClientOptions clientOptions =
        new WebClientOptions()
            .setDefaultPort(config.getDownstreamHttpPort())
            .setDefaultHost(config.getDownstreamHttpHost());
    final HttpServerOptions serverOptions =
        new HttpServerOptions()
            .setPort(config.getHttpListenPort())
            .setHost(config.getHttpListenHost())
            .setReuseAddress(true)
            .setReusePort(true);

    final Runner runner =
        new Runner(
            config.getChainId().id(),
            transactionSignerProvider,
            applyConfigTlsSettingsTo(clientOptions),
            applyConfigTlsSettingsTo(serverOptions),
            downstreamHttpRequestTimeout,
            jsonDecoder,
            config.getDataPath());

    runner.start();
  }

  private WebClientOptions applyConfigTlsSettingsTo(final WebClientOptions input) {
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    if (optionalDownstreamTlsOptions.isEmpty()
        || !optionalDownstreamTlsOptions.get().isTlsEnabled()) {
      return input;
    }

    final WebClientOptions result = new WebClientOptions(input);
    final ClientTlsOptions clientTlsOptions = optionalDownstreamTlsOptions.get();
    result.setSsl(true);

    applyDownstreamTlsTrustOptions(result, clientTlsOptions.getClientTlsTrustOptions());
    applyDownstreamClientAuthOptions(result, clientTlsOptions.getClientTlsCertificateOptions());

    return result;
  }

  private void applyDownstreamTlsTrustOptions(
      final WebClientOptions result,
      final Optional<ClientTlsTrustOptions> optionalDownstreamTrustOptions) {

    if (optionalDownstreamTrustOptions.isPresent()) {
      final Optional<Path> optionalKnownServerFile =
          optionalDownstreamTrustOptions.get().getKnownServerFile();
      if (optionalKnownServerFile.isPresent()) {
        final Path knownServerFile = optionalKnownServerFile.get();
        try {
          result.setTrustOptions(
              whitelistServers(
                  knownServerFile,
                  optionalDownstreamTrustOptions.get().isCaSignedServerCertificateAllowed()));
        } catch (RuntimeException e) {
          throw new InitializationException("Failed to load known server file.", e);
        }
      } else if (!optionalDownstreamTrustOptions.get().isCaSignedServerCertificateAllowed()) {
        throw new InitializationException(
            "Must specify a known-server file if CA-signed option is disabled");
      }
    }
  }

  private void applyDownstreamClientAuthOptions(
      final WebClientOptions result,
      final Optional<ClientTlsCertificateOptions> optionalDownstreamTlsClientAuthOptions) {
    if (optionalDownstreamTlsClientAuthOptions.isPresent()) {
      try {
        result.setPfxKeyCertOptions(convertFrom(optionalDownstreamTlsClientAuthOptions.get()));
      } catch (final IOException e) {
        throw new InitializationException("Failed to load client certificate.", e);
      }
    }
  }

  private static PfxOptions convertFrom(final ClientTlsCertificateOptions pkcsConfig)
      throws IOException {
    final String password = readSecretFromFile(pkcsConfig.getKeyStorePasswordFile());
    return new PfxOptions().setPassword(password).setPath(pkcsConfig.getKeyStoreFile().toString());
  }

  private HttpServerOptions applyConfigTlsSettingsTo(final HttpServerOptions input) {

    if (config.getTlsOptions().isEmpty()) {
      return input;
    }

    HttpServerOptions result = new HttpServerOptions(input);
    result.setSsl(true);
    final TlsOptions tlsConfig = config.getTlsOptions().get();

    result = applyTlsKeyStore(result, tlsConfig);

    if (tlsConfig.getClientAuthConstraints().isPresent()) {
      result = applyClientAuthentication(result, tlsConfig.getClientAuthConstraints().get());
    }

    return result;
  }

  private static HttpServerOptions applyTlsKeyStore(
      final HttpServerOptions input, final TlsOptions tlsConfig) {
    final HttpServerOptions result = new HttpServerOptions(input);

    try {
      final String keyStorePathname =
          tlsConfig.getKeyStoreFile().toPath().toAbsolutePath().toString();
      final String password = readSecretFromFile(tlsConfig.getKeyStorePasswordFile().toPath());
      result.setPfxKeyCertOptions(new PfxOptions().setPath(keyStorePathname).setPassword(password));
      return result;
    } catch (final NoSuchFileException e) {
      throw new InitializationException(
          "Requested file " + e.getMessage() + " does not exist at specified location.", e);
    } catch (final AccessDeniedException e) {
      throw new InitializationException(
          "Current user does not have permissions to access " + e.getMessage(), e);
    } catch (final IOException e) {
      throw new InitializationException("Failed to load TLS files " + e.getMessage(), e);
    }
  }

  private static HttpServerOptions applyClientAuthentication(
      final HttpServerOptions input, final ClientAuthConstraints constraints) {
    final HttpServerOptions result = new HttpServerOptions(input);

    result.setClientAuth(ClientAuth.REQUIRED);
    try {
      constraints
          .getKnownClientsFile()
          .ifPresent(
              whitelistFile ->
                  result.setTrustOptions(
                      VertxTrustOptions.whitelistClients(
                          whitelistFile.toPath(), constraints.isCaAuthorizedClientAllowed())));
    } catch (final IllegalArgumentException e) {
      throw new InitializationException("Illegally formatted client fingerprint file.");
    }

    return result;
  }

  public static JsonDecoder createJsonDecoder() {
    // Force Transaction Deserialization to fail if missing expected properties
    final ObjectMapper jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    return new JsonDecoder(jsonObjectMapper);
  }

  private static String readSecretFromFile(final Path path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(path);
    return new String(fileContent, Charsets.UTF_8);
  }
}

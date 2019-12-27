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

import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.net.tls.VertxTrustOptions;
import org.web3j.protocol.http.HttpService;

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

    final HttpService web3jService = createWeb3jHttpService();
    final TransactionFactory transactionFactory =
        TransactionFactory.createFrom(web3jService, jsonDecoder);
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
            clientOptions,
            applyConfigTlsSettingsTo(serverOptions),
            downstreamHttpRequestTimeout,
            transactionFactory,
            jsonDecoder,
            config.getDataPath());

    runner.start();
  }

  private HttpServerOptions applyConfigTlsSettingsTo(final HttpServerOptions input) {

    if (config.getTlsOptions().isEmpty()) {
      return input;
    }

    final HttpServerOptions result = new HttpServerOptions(input);
    result.setSsl(true);
    final TlsOptions tlsConfig = config.getTlsOptions().get();
    try {
      final String keyStorePathname =
          tlsConfig.getKeyStoreFile().toPath().toAbsolutePath().toString();
      final String password = readSecretFromFile(tlsConfig.getKeyStorePasswordFile().toPath());
      result.setPfxKeyCertOptions(new PfxOptions().setPath(keyStorePathname).setPassword(password));

      if (tlsConfig.getWhitelistFingerprints().isPresent()) {
        // result.setClientAuth(ClientAuth.REQUIRED);
        result.setTrustOptions(
            VertxTrustOptions.whitelistClients(
                tlsConfig.getWhitelistFingerprints().get().toPath()));
      }
    } catch (final NoSuchFileException e) {
      throw new InitialisationException(
          "Requested file " + e.getMessage() + " does not exist at specified location.", e);
    } catch (final AccessDeniedException e) {
      throw new InitialisationException(
          "Current user does not have permissions to access " + e.getMessage(), e);
    } catch (final IOException e) {
      throw new InitialisationException("Failed to load TLS files " + e.getMessage(), e);
    }

    return result;
  }

  public static JsonDecoder createJsonDecoder() {
    // Force Transaction Deserialisation to fail if missing expected properties
    final ObjectMapper jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    return new JsonDecoder(jsonObjectMapper);
  }

  private HttpService createWeb3jHttpService() {
    final String downstreamUrl =
        "http://" + config.getDownstreamHttpHost() + ":" + config.getDownstreamHttpPort();
    LOG.info("Downstream URL = {}", downstreamUrl);

    final OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder
        .connectTimeout(config.getDownstreamHttpRequestTimeout())
        .readTimeout(config.getDownstreamHttpRequestTimeout());
    return new HttpService(downstreamUrl, builder.build());
  }

  private static String readSecretFromFile(final Path path) throws IOException {
    final byte[] fileContent = Files.readAllBytes(path);
    return new String(fileContent, Charsets.UTF_8);
  }
}

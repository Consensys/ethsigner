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

import static org.apache.tuweni.net.tls.VertxTrustOptions.whitelistServers;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.signer.filebased.CredentialTransactionSigner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;

public class HashicorpVaultSignerFactory {

  private static final Logger LOG = LogManager.getLogger();
  private static final String HASHICORP_SECRET_ENGINE_VERSION = "/v1";
  private static final String AUTH_FILE_MESSAGE =
      "Unable to read file containing the authentication information for Hashicorp Vault: ";
  private static final String RETRIEVE_PRIVATE_KEY_MESSAGE =
      "Unable to retrieve private key from Hashicorp Vault.";
  private static final String TIMEOUT_MESSAGE =
      "Timeout while retrieving private key from Hashicorp Vault.";

  private HashicorpConfig hashicorpConfig;

  public TransactionSigner createSigner(final HashicorpConfig hashicorpConfig) {
    this.hashicorpConfig = hashicorpConfig;
    final String response = requestSecretFromVault();
    final Credentials credentials = extractCredentialsFromJson(response);
    if (credentials.getAddress() != null) {
      LOG.debug("Successfully retrieved the credentials from the Hashicorp vault.");
    }
    return new CredentialTransactionSigner(credentials);
  }

  private String requestSecretFromVault() {
    final String requestURI = HASHICORP_SECRET_ENGINE_VERSION + hashicorpConfig.getSigningKeyPath();

    return getVaultResponse(
        hashicorpConfig.getPort(),
        hashicorpConfig.getHost(),
        hashicorpConfig.getAuthFilePath(),
        requestURI,
        hashicorpConfig.getTimeout());
  }

  private String getVaultResponse(
      final int serverPort,
      final String serverHost,
      final Path authFilePath,
      final String requestURI,
      final long timeout) {
    final Vertx vertx = Vertx.vertx();
    try {
      final HttpClient httpClient =
          vertx.createHttpClient(getHttpClientOptions(serverHost, serverPort));

      final CompletableFuture<String> future = new CompletableFuture<>();
      final HttpClientRequest request =
          httpClient.request(
              HttpMethod.GET,
              requestURI,
              rh ->
                  rh.bodyHandler(
                      bh -> {
                        if (rh.statusCode() == 200) {
                          future.complete(bh.toString());
                        } else {
                          future.completeExceptionally(
                              new Exception(
                                  "Hashicorp vault responded with status code {"
                                      + rh.statusCode()
                                      + "}"));
                        }
                      }));
      request.headers().set("X-Vault-Token", readTokenFromFile(authFilePath));
      request.setChunked(false);
      request.end();
      return getResponse(future, timeout);
    } finally {
      if (vertx != null) {
        vertx.close();
      }
    }
  }

  private HttpClientOptions getHttpClientOptions(final String serverHost, final int serverPort) {
    final HttpClientOptions httpClientOptions =
        new HttpClientOptions()
            .setDefaultHost(serverHost)
            .setDefaultPort(serverPort)
            .setSsl(hashicorpConfig.isTlsEnabled());

    hashicorpConfig
        .getTlsKnownServerFile()
        .ifPresent(
            knownServerFile ->
                httpClientOptions.setTrustOptions(whitelistServers(knownServerFile)));

    return httpClientOptions;
  }

  private static String readTokenFromFile(final Path path) {
    final List<String> authFileLines;
    try {
      authFileLines = Files.readAllLines(path);
    } catch (final IOException e) {
      final String message = AUTH_FILE_MESSAGE + path;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    }
    return authFileLines.get(0);
  }

  private static String getResponse(final CompletableFuture<String> future, final long timeout) {
    final String response;
    try {
      response = future.get(timeout, TimeUnit.SECONDS);
    } catch (final InterruptedException | ExecutionException e) {
      final String message = RETRIEVE_PRIVATE_KEY_MESSAGE;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    } catch (final TimeoutException e) {
      final String message = TIMEOUT_MESSAGE;
      LOG.error(message, e);
      throw new TransactionSignerInitializationException(message, e);
    }
    return response;
  }

  private static Credentials extractCredentialsFromJson(final String response) {
    if (response == null) {
      return null;
    }
    final JsonObject jsonObject = new JsonObject(response);
    final String privateKeyHex =
        jsonObject.getJsonObject("data").getJsonObject("data").getString("value");
    return Credentials.create(privateKeyHex);
  }
}

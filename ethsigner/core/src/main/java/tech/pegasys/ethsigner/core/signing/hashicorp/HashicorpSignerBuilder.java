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
package tech.pegasys.ethsigner.core.signing.hashicorp;

import tech.pegasys.ethsigner.core.signing.CredentialTransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;

public class HashicorpSignerBuilder {

  private static final Logger LOG = LogManager.getLogger();
  private static final String HASHICORP_SECRET_ENGINE_VERSION = "/v1";

  private final HttpClient hashicorpVaultClient;
  private final HashicorpSignerConfig config;

  public HashicorpSignerBuilder(final HashicorpSignerConfig config, final Vertx vertx) {
    this.config = config;
    this.hashicorpVaultClient = vertx.createHttpClient();
  }

  public TransactionSigner build() {
    final String tokenString = readTokenFromFile();
    if (tokenString == null) {
      return null;
    }
    final String response = requestSecretFromVault(tokenString);
    if (response == null) {
      return null;
    }
    final Credentials credentials = extractCredentialsFromJson(response);
    if (credentials == null) {
      return null;
    }
    return new CredentialTransactionSigner(credentials);
  }

  private String readTokenFromFile() {
    final List<String> authFileLines;
    try {
      authFileLines = Files.readAllLines(config.getAuthFilePath());
    } catch (final IOException e) {
      LOG.error(
          "Unable to read file containing the authentication information for Hashicorp Vault: "
              + config.getAuthFilePath(),
          e);
      return null;
    }
    return authFileLines.get(0);
  }

  private String requestSecretFromVault(final String tokenString) {
    final String requestURI = HASHICORP_SECRET_ENGINE_VERSION + config.getSigningKeyPath();

    return getVaultResponse(tokenString, requestURI);
  }

  private String getVaultResponse(final String tokenString, final String requestURI) {
    final CompletableFuture<String> future = new CompletableFuture<>();
    final HttpClientRequest request =
        hashicorpVaultClient.request(
            HttpMethod.GET,
            config.getServerPort(),
            config.getServerHost(),
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
    if (tokenString != null) {
      request.headers().set("X-Vault-Token", tokenString);
    }
    request.setChunked(false);
    request.end();
    return getResponse(future);
  }

  private String getResponse(final CompletableFuture<String> future) {
    final String response;
    try {
      response = future.get(config.getTimeout(), TimeUnit.SECONDS);
    } catch (final InterruptedException | ExecutionException e) {
      LOG.error(
          "Unable to retrieve private key from Hashicorp Vault with this config: \n" + config, e);
      return null;
    } catch (final TimeoutException e) {
      LOG.error(
          "Timeout while retrieving private key from Hashicorp Vault with this config: \n" + config,
          e);
      return null;
    }
    return response;
  }

  private Credentials extractCredentialsFromJson(final String response) {
    if (response == null) {
      return null;
    }
    final JsonObject jsonObject = new JsonObject(response);
    final String privateKeyHex =
        jsonObject.getJsonObject("data").getJsonObject("data").getString("value");
    return Credentials.create(privateKeyHex);
  }
}

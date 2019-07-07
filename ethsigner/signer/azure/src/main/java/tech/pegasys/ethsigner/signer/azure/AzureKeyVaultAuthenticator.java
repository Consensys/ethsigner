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

import tech.pegasys.ethsigner.TransactionSignerInitializationException;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.KeyVaultClientCustom;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AzureKeyVaultAuthenticator {

  private static final Logger LOG = LogManager.getLogger();
  public static final String AUTHENTICATION_FAILURE_MESSAGE =
      "Failed to get token from Azure vault";

  public KeyVaultClientCustom getAuthenticatedClient(
      final String clientId, final String clientSecret) {
    final ClientCredential credentials = new ClientCredential(clientId, clientSecret);
    return new KeyVaultClient(createKeyVaultClient(credentials));
  }

  private ServiceClientCredentials createKeyVaultClient(final ClientCredential credentials) {
    return new KeyVaultCredentials() {

      // Callback that supplies the token type and access token on request.
      @Override
      @SuppressWarnings("CatchAndPrintStackTrace")
      public String doAuthenticate(
          final String authorization, final String resource, final String scope) {

        try {
          final AuthenticationResult authResult =
              getAccessToken(authorization, resource, credentials);
          return authResult.getAccessToken();
        } catch (final Exception e) {
          LOG.error(AUTHENTICATION_FAILURE_MESSAGE, e);
          throw new TransactionSignerInitializationException(AUTHENTICATION_FAILURE_MESSAGE, e);
        }
      }
    };
  }

  /**
   * Private helper method that gets the access token for the authorization and resource depending
   * on which variables are supplied in the environment.
   */
  private AuthenticationResult getAccessToken(
      final String authorization, final String resource, final ClientCredential credentials)
      throws InterruptedException, ExecutionException, MalformedURLException {

    AuthenticationResult result = null;

    // Starts a service to fetch access token.
    final ExecutorService service = Executors.newFixedThreadPool(1);
    try {
      final AuthenticationContext context =
          new AuthenticationContext(authorization, false, service);
      final Future<AuthenticationResult> future = context.acquireToken(resource, credentials, null);
      result = future.get();
    } finally {
      service.shutdown();
    }

    if (result == null) {
      throw new RuntimeException("Authentication results were null.");
    }
    return result;
  }
}

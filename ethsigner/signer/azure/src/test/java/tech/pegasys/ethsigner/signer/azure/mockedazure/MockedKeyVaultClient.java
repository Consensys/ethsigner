package tech.pegasys.ethsigner.signer.azure.mockedazure;

import com.microsoft.azure.keyvault.implementation.KeyVaultClientCustomImpl;
import com.microsoft.rest.credentials.ServiceClientCredentials;

public class MockedKeyVaultClient extends KeyVaultClientCustomImpl {

  protected MockedKeyVaultClient(
      ServiceClientCredentials credentials) {
    super(credentials);
  }
}

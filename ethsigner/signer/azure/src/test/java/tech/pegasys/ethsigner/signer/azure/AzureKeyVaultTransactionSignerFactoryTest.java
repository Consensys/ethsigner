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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.math.BigInteger;
import java.util.Arrays;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

@RunWith(MockitoJUnitRunner.class)
public class AzureKeyVaultTransactionSignerFactoryTest {

  @Test
  public void generatedTransactionSignerHasExpectedAddress() {
    final ECKeyPair web3jKeyPair = ECKeyPair.create(BigInteger.valueOf(5));
    final String expectedAddress = Credentials.create(web3jKeyPair).getAddress();

    final String keyName = "keyName";
    final String keyVersion = "keyVersion";
    final KeyVaultClient mockClient = mock(KeyVaultClient.class);
    final KeyBundle mockKeyBundle = mock(KeyBundle.class);
    final JsonWebKey mockWebKey = mock(JsonWebKey.class);
    when(mockWebKey.x())
        .thenReturn(Arrays.copyOfRange(web3jKeyPair.getPublicKey().toByteArray(), 0, 32));
    when(mockWebKey.y())
        .thenReturn(Arrays.copyOfRange(web3jKeyPair.getPublicKey().toByteArray(), 32, 64));
    when(mockClient.getKey(any())).thenReturn(mockKeyBundle);
    when(mockKeyBundle.key()).thenReturn(mockWebKey);

    AzureKeyVaultTransactionSignerFactory factory =
        new AzureKeyVaultTransactionSignerFactory("arbitraryKeyVault", mockClient);
    TransactionSigner signer = factory.createSigner(keyName, keyVersion);
    assertThat(signer.getAddress()).isEqualTo(expectedAddress);
  }
}

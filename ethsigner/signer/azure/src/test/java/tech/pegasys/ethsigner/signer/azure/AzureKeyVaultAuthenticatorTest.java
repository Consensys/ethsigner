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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyIdentifier;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyItem;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

public class AzureKeyVaultAuthenticatorTest {

  private static final String clientID = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
  private static final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");

  private static final String validKeyVersion = "63242cd20e7144039611d56054feff9e";

  private final KeyVaultClient client =
      AzureKeyVaultAuthenticator.getAuthenticatedClient(clientID, clientSecret);

  @Test
  public void ensureCanAuthenticateAndFindKeys() {
    assertThat(client.apiVersion()).isEqualTo("7.0");

    PagedList<KeyItem> keys = client.listKeys("https://ethsignertestkey.vault.azure.net");
    assertThat(keys.size()).isEqualTo(1);

    final KeyItem keyItem = keys.get(0);
    assertThat(keyItem.kid()).isEqualTo("https://ethsignertestkey.vault.azure.net/keys/TestKey");

    KeyIdentifier kid = new KeyIdentifier(keyItem.kid());
    assertThat(kid.name()).isEqualTo("TestKey");

    final PagedList<KeyItem> keyVersions =
        client.listKeyVersions("https://ethsignertestkey.vault.azure.net", "TestKey");
    assertThat(keyVersions.size()).isEqualTo(1);

    KeyItem keyVersion = keyVersions.get(0);
    assertThat(keyVersion.kid())
        .isEqualTo(
            "https://ethsignertestkey.vault.azure.net/keys/TestKey/63242cd20e7144039611d56054feff9e");

    kid = new KeyIdentifier(keyVersion.kid());
    assertThat(kid.version()).isEqualTo(validKeyVersion);

    final KeyBundle key = client.getKey(keyVersion.kid());
    assertThat(key.key().kty()).isEqualTo(JsonWebKeyType.EC);
    assertThat(key.key().crv().toString()).isEqualTo("SECP256K1");
    assertThat(Hex.encodeHexString(key.key().x()))
        .isEqualTo("09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792");
    assertThat(Hex.encodeHexString(key.key().y()))
        .isEqualTo("f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842");
  }

  @Test
  public void ensureCanFindKeysAndSign() {

    final AzureKeyVaultTransactionSignerFactory factory =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", client);

    final TransactionSigner signer = factory.createSigner("TestKey", validKeyVersion);
    assertThat(signer.getAddress()).isEqualTo("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73");

    byte[] data = {1, 2, 3};
    signer.sign(data);
  }

  @Test
  public void accessingNonExistentKeyVaultThrowsExceptionWithMessage() {
    final String vaultName = "invalidKeyVault";
    final AzureKeyVaultTransactionSignerFactory nonExistentKeyVaultFactory =
        new AzureKeyVaultTransactionSignerFactory(vaultName, client);

    final String expectedMessage =
        String.format(
            AzureKeyVaultTransactionSignerFactory.INVALID_VAULT_PARAMETERS_ERROR_PATTERN,
            AzureKeyVaultTransactionSignerFactory.constructAzureKeyVaultUrl(vaultName));

    assertThatThrownBy(() -> nonExistentKeyVaultFactory.createSigner("TestKey", validKeyVersion))
        .isInstanceOf(TransactionSignerInitializationException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  public void accessingIncorrectKeyNameOrValueThrowsExceptionWithMessage() {
    final AzureKeyVaultTransactionSignerFactory validFactory =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", client);

    assertThatThrownBy(() -> validFactory.createSigner("TestKey", "invalid_version"))
        .isInstanceOf(TransactionSignerInitializationException.class)
        .hasMessage(AzureKeyVaultTransactionSignerFactory.INVALID_KEY_PARAMETERS_ERROR);

    assertThatThrownBy(() -> validFactory.createSigner("invalid_keyname", validKeyVersion))
        .isInstanceOf(TransactionSignerInitializationException.class)
        .hasMessage(AzureKeyVaultTransactionSignerFactory.INVALID_KEY_PARAMETERS_ERROR);
  }

  @Test
  public void invalidClientCredentialsResultInException() {
    final KeyVaultClient clientWithInvalidId =
        AzureKeyVaultAuthenticator.getAuthenticatedClient("Invalid_id", clientSecret);
    final AzureKeyVaultTransactionSignerFactory factoryWithInvalidClientId =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", clientWithInvalidId);

    assertThatThrownBy(() -> factoryWithInvalidClientId.createSigner("TestKey", validKeyVersion))
        .isInstanceOf(TransactionSignerInitializationException.class)
        .hasMessage(AzureKeyVaultTransactionSignerFactory.INACCESSIBLE_KEY_ERROR);

    final KeyVaultClient clientWithInvalidSecret =
        AzureKeyVaultAuthenticator.getAuthenticatedClient(clientID, "invalid_secret");
    final AzureKeyVaultTransactionSignerFactory factoryWithInvalidClientSecret =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", clientWithInvalidSecret);

    assertThatThrownBy(
            () -> factoryWithInvalidClientSecret.createSigner("TestKey", validKeyVersion))
        .isInstanceOf(TransactionSignerInitializationException.class)
        .hasMessage(AzureKeyVaultTransactionSignerFactory.INACCESSIBLE_KEY_ERROR);
  }
  /*
    @Test
    public void importKeyToAzure() {
      final String privKeyStr = "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63".toUpperCase();

      final BigInteger privKey = new BigInteger(1, BaseEncoding.base16().decode(privKeyStr));
      final ECKeyPair keyPair = ECKeyPair.create(privKey);

      JsonWebKey webKey = new JsonWebKey();
      webKey.withD(keyPair.getPrivateKey().toByteArray());
      webKey.withX(Arrays.copyOfRange(keyPair.getPublicKey().toByteArray(), 0, 32));
      webKey.withY(Arrays.copyOfRange(keyPair.getPublicKey().toByteArray(), 32, 64));
      webKey.withKty(JsonWebKeyType.EC);
      webKey.withCrv(new JsonWebKeyCurveName("SECP256K1"));
      webKey.withKeyOps(Lists.newArrayList(JsonWebKeyOperation.SIGN, JsonWebKeyOperation.VERIFY));

  //    client.importKey("https://ethsignertestkey.vault.azure.net", "TestKey", webKey);
    }
    */

}

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
import static org.web3j.crypto.Keys.getAddress;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.Signature;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.math.BigInteger;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyIdentifier;
import com.microsoft.azure.keyvault.KeyVaultClientCustom;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyItem;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import org.apache.commons.codec.binary.Hex;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

@Ignore
public class AzureKeyVaultAuthenticatorTest {

  private static final String clientId = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
  private static final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");

  private static final String validKeyVersion = "7c01fe58d68148bba5824ce418241092";

  private final AzureKeyVaultAuthenticator authenticator = new AzureKeyVaultAuthenticator();
  private final KeyVaultClientCustom client =
      authenticator.getAuthenticatedClient(clientId, clientSecret);

  @Test
  public void ensureCanAuthenticateAndFindKeys() {
    assertThat(client.apiVersion()).isEqualTo("7.0");

    final PagedList<KeyItem> keys = client.listKeys("https://ethsignertestkey.vault.azure.net");
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
        .isEqualTo("https://ethsignertestkey.vault.azure.net/keys/TestKey/" + validKeyVersion);

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
    final String EXPECTED_ADDRESS = "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73";

    final AzureKeyVaultTransactionSignerFactory factory =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", client);

    final TransactionSigner signer = factory.createSigner("TestKey", validKeyVersion);
    assertThat(signer.getAddress()).isEqualTo(EXPECTED_ADDRESS);

    byte[] data = {1, 2, 3};
    final Signature signature = signer.sign(data);
    assertThat(signature).isNotNull();

    byte[] dataHash = Hash.sha3(data);
    final BigInteger publicKey =
        Sign.recoverFromSignature(
            signature.getV().intValue() - 27,
            new ECDSASignature(signature.getR(), signature.getS()),
            dataHash);

    assertThat("0x" + getAddress(publicKey)).isEqualTo(signer.getAddress());
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
    final KeyVaultClientCustom clientWithInvalidId =
        authenticator.getAuthenticatedClient("Invalid_id", clientSecret);
    final AzureKeyVaultTransactionSignerFactory factoryWithInvalidClientId =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", clientWithInvalidId);

    assertThatThrownBy(() -> factoryWithInvalidClientId.createSigner("TestKey", validKeyVersion))
        .isInstanceOf(TransactionSignerInitializationException.class)
        .hasMessage(AzureKeyVaultTransactionSignerFactory.INACCESSIBLE_KEY_ERROR);

    final KeyVaultClientCustom clientWithInvalidSecret =
        authenticator.getAuthenticatedClient(clientId, "invalid_secret");
    final AzureKeyVaultTransactionSignerFactory factoryWithInvalidClientSecret =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", clientWithInvalidSecret);

    assertThatThrownBy(
            () -> factoryWithInvalidClientSecret.createSigner("TestKey", validKeyVersion))
        .isInstanceOf(TransactionSignerInitializationException.class)
        .hasMessage(AzureKeyVaultTransactionSignerFactory.INACCESSIBLE_KEY_ERROR);
  }

  @Test
  public void nullClientAndOrSecretAreHandledCleanly() {
    assertThatThrownBy(() -> authenticator.getAuthenticatedClient(null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

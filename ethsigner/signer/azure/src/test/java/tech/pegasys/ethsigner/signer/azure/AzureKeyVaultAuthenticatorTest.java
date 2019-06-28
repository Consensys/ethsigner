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

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyIdentifier;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyItem;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyCurveName;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import org.junit.Test;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

public class AzureKeyVaultAuthenticatorTest {

  @Test
  public void ensureCanAuthenticateAndFindKeys() {
    KeyVaultClient client = AzureKeyVaultAuthenticator.getAuthenticatedClient(
        "add your client ID here",
        "add your client secret here");

    assertThat(client.apiVersion()).isEqualTo("7.0");

    PagedList<KeyItem> keys = client.listKeys("https://photic-kv-test.vault.azure.net");
    assertThat(keys.size()).isEqualTo(1);

    KeyItem keyItem = keys.get(0);
    assertThat(keyItem.kid())
        .isEqualTo("https://photic-kv-test.vault.azure.net/keys/eth-signing-key-1");

    KeyIdentifier kid = new KeyIdentifier(keyItem.kid());
    assertThat(kid.name()).isEqualTo("eth-signing-key-1");

    PagedList<KeyItem> keyVersions =
        client.listKeyVersions("https://photic-kv-test.vault.azure.net", "eth-signing-key-1");
    assertThat(keyVersions.size()).isEqualTo(1);

    KeyItem keyVersion = keyVersions.get(0);
    assertThat(keyVersion.kid()).isEqualTo(
        "https://photic-kv-test.vault.azure.net/keys/eth-signing-key-1/3c63feb0d41d458b9c02c8d23a6b3e88");

    kid = new KeyIdentifier(keyVersion.kid());
    assertThat(kid.version()).isEqualTo("3c63feb0d41d458b9c02c8d23a6b3e88");

    KeyBundle key = client.getKey(keyVersion.kid());
    assertThat(key.key().kty()).isEqualTo(JsonWebKeyType.EC);
    assertThat(key.key().crv()).isEqualTo(new JsonWebKeyCurveName("SECP256K1"));
    assertThat(bytesToHex(key.key().x()))
        .isEqualTo("440c9b19904eb55c245bfee4d53f8b771c9596c9d64819c1439612c9e5662f4a");
    assertThat(bytesToHex(key.key().y()))
        .isEqualTo("ff19750609d996e9b47923706a8dd04f8d3746e8b44bb68d1ede44e22a541aa3");
  }

  @Test
  public void ensureCanFindKeysAndSign() {
    final KeyVaultClient client = AzureKeyVaultAuthenticator.getAuthenticatedClient(
        "add your client ID here",
        "add your client secret here");

    final AzureKeyVaultTransactionSignerFactory factory =
        new AzureKeyVaultTransactionSignerFactory("photic-kv-test", client);

    final TransactionSigner signer = factory.createSigner(null, null);
    assertThat(signer.getAddress()).isEqualTo("0xbbde9116b300e92e2798e28a90acb3b16b357179");

    byte[] data = {1, 2, 3};
    signer.sign(data);
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}

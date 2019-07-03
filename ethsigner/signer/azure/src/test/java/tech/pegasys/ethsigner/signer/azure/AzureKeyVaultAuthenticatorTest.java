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

  private static final String clientID = "47efee5c-8079-4b48-96a7-31bb4f2e9ae2";
  private static final String clientSecret = "TW_3Uc/GLDdpLp5*om@MGcdlT29MuP*5";

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
            "https://ethsignertestkey.vault.azure.net/keys/TestKey/449e655872f145a795f0849828685848");

    kid = new KeyIdentifier(keyVersion.kid());
    assertThat(kid.version()).isEqualTo("449e655872f145a795f0849828685848");

    final KeyBundle key = client.getKey(keyVersion.kid());
    assertThat(key.key().kty()).isEqualTo(JsonWebKeyType.EC);
    assertThat(key.key().crv().toString()).isEqualTo("SECP256K1");
    assertThat(Hex.encodeHexString(key.key().x()))
        .isEqualTo("f12426be201197a20a9ac966c4222acad79a3d61d7ec7b2a52d7320422354e95");
    assertThat(Hex.encodeHexString(key.key().y()))
        .isEqualTo("36741ae914de1aae3aa344da1dce53a07a1b76d59035a0c7e29049574969a17a");
  }

  @Test
  public void ensureCanFindKeysAndSign() {

    final AzureKeyVaultTransactionSignerFactory factory =
        new AzureKeyVaultTransactionSignerFactory("ethsignertestkey", client);

    final TransactionSigner signer =
        factory.createSigner("TestKey", "449e655872f145a795f0849828685848");
    assertThat(signer.getAddress()).isEqualTo("0x8c22e8c8665f3b574e45ae89fc0434eddc286409");

    byte[] data = {1, 2, 3};
    signer.sign(data);
  }
}

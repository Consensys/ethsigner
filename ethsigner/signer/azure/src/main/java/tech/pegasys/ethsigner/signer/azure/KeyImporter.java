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

import java.math.BigInteger;
import java.util.Arrays;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.microsoft.azure.keyvault.KeyVaultClientCustom;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyCurveName;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyOperation;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import org.web3j.crypto.ECKeyPair;

public class KeyImporter {

  // Creates a SECP256k1 key (usable for Ethereum signing) in Azure KeyVault.
  public static void importKeyToCloudVault() {
    final String clientId = System.getenv("ETHSIGNER_AZURE_CLIENT_ID");
    final String clientSecret = System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET");
    final String keyVaultName = "ethsignertestkey";
    final String keyName = "TestKey";

    final AzureKeyVaultAuthenticator authenticator = new AzureKeyVaultAuthenticator();
    final KeyVaultClientCustom client =
        authenticator.getAuthenticatedClient(clientId, clientSecret);

    final String privKeyStr =
        "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63".toUpperCase();

    final BigInteger privKey = new BigInteger(1, BaseEncoding.base16().decode(privKeyStr));
    final ECKeyPair keyPair = ECKeyPair.create(privKey);

    final JsonWebKey webKey = new JsonWebKey();
    webKey.withD(keyPair.getPrivateKey().toByteArray());
    webKey.withX(Arrays.copyOfRange(keyPair.getPublicKey().toByteArray(), 0, 32));
    webKey.withY(Arrays.copyOfRange(keyPair.getPublicKey().toByteArray(), 32, 64));
    webKey.withKty(JsonWebKeyType.EC);
    webKey.withCrv(new JsonWebKeyCurveName("SECP256K1"));
    webKey.withKeyOps(Lists.newArrayList(JsonWebKeyOperation.SIGN, JsonWebKeyOperation.VERIFY));
    client.importKey(
        AzureKeyVaultTransactionSignerFactory.constructAzureKeyVaultUrl(keyVaultName),
        keyName,
        webKey);
  }
}

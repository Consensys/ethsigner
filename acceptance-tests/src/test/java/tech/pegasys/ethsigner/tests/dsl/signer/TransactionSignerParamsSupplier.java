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
package tech.pegasys.ethsigner.tests.dsl.signer;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.tests.dsl.Accounts;
import tech.pegasys.ethsigner.tests.hashicorpvault.HashicorpVaultDocker;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.io.Resources;

public class TransactionSignerParamsSupplier {

  private final int hashicorpVaultPort;
  private final String ipAddress;
  private final String azureKeyVault;
  private final Path multiPlatformSignerDirectory;

  public TransactionSignerParamsSupplier(
      final int hashicorpVaultPort,
      final String ipAddress,
      final String azureKeyVault,
      final Path multiPlatformSignerDirectory) {
    this.hashicorpVaultPort = hashicorpVaultPort;
    this.ipAddress = ipAddress;
    this.azureKeyVault = azureKeyVault;
  }

  public Collection<String> get() {
    final ArrayList<String> params = new ArrayList<>();
    if (hashicorpVaultPort != 0) {
      params.add("hashicorp-signer");
      params.add("--auth-file");
      params.add(createVaultAuthFile().getAbsolutePath());
      params.add("--host");
      params.add(ipAddress);
      params.add("--port");
      params.add(String.valueOf(hashicorpVaultPort));
    } else if (azureKeyVault != null) {
      params.add("azure-signer");
      params.add("--key-vault-name");
      params.add(azureKeyVault);
      params.add("--key-name");
      params.add("TestKey");
      params.add("--key-version");
      params.add("7c01fe58d68148bba5824ce418241092");
      params.add("--client-id");
      params.add(System.getenv("ETHSIGNER_AZURE_CLIENT_ID"));
      params.add("--client-secret-path");
      params.add(createAzureSecretFile().getAbsolutePath());
    } else if (multiPlatformSignerDirectory != null) {
      params.add("multiplatform-signer");
      params.add("--directory");
      params.add(multiPlatformSignerDirectory.toAbsolutePath().toString());
    } else {
      params.add("file-based-signer");
      params.add("--password-file");
      params.add(createPasswordFile().getAbsolutePath());
      params.add("--key-file");
      params.add(createKeyFile().getAbsolutePath());
    }
    return params;
  }

  private File createAzureSecretFile() {
    return createTmpFile(
        "azure_secret", System.getenv("ETHSIGNER_AZURE_CLIENT_SECRET").getBytes(UTF_8));
  }

  private File createPasswordFile() {
    return createTmpFile(
        "ethsigner_passwordfile", Accounts.GENESIS_ACCOUNT_ONE_PASSWORD.getBytes(UTF_8));
  }

  @SuppressWarnings("UnstableApiUsage")
  private File createKeyFile() {
    final URL resource = Resources.getResource("rich_benefactor_one.json");
    final byte[] data;

    try {
      data = Resources.toString(resource, UTF_8).getBytes(UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    return createTmpFile("ethsigner_keyfile", data);
  }

  private File createVaultAuthFile() {
    return createTmpFile("vault_authfile", HashicorpVaultDocker.vaultToken.getBytes(UTF_8));
  }

  private File createTmpFile(final String tempNamePrefix, final byte[] data) {
    final Path path;
    try {
      path = Files.createTempFile(tempNamePrefix, null);
      Files.write(path, data);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final File tmpFile = path.toFile();
    tmpFile.deleteOnExit();
    return tmpFile;
  }
}

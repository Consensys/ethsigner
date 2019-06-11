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

  public TransactionSignerParamsSupplier(final int hashicorpVaultPort, final String ipAddress) {
    this.hashicorpVaultPort = hashicorpVaultPort;
    this.ipAddress = ipAddress;
  }

  public Collection<String> get() {
    final ArrayList<String> params = new ArrayList<>();
    if (hashicorpVaultPort == 0) {
      params.add("file-based-signer");
      params.add("--password-file");
      params.add(createPasswordFile().getAbsolutePath());
      params.add("--key-file");
      params.add(createKeyFile().getAbsolutePath());
    } else {
      params.add("hashicorp-signer");
      params.add("--auth-file");
      params.add(createVaultAuthFile().getAbsolutePath());
      params.add("--host");
      params.add(ipAddress);
      params.add("--port");
      params.add(String.valueOf(hashicorpVaultPort));
    }
    return params;
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
    final Path file;
    try {
      file = Files.createTempFile(tempNamePrefix, ".file");
      Files.write(file, data);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final File tmpFile = file.toFile();
    tmpFile.deleteOnExit();
    return tmpFile;
  }
}

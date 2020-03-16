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
import static tech.pegasys.signers.hashicorp.dsl.certificates.CertificateHelpers.createFingerprintFile;

import tech.pegasys.ethsigner.tests.dsl.Accounts;
import tech.pegasys.ethsigner.tests.dsl.node.HashicorpSigningParams;
import tech.pegasys.signers.hashicorp.dsl.certificates.SelfSignedCertificate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.google.common.io.Resources;

public class TransactionSignerParamsSupplier {

  private final HashicorpSigningParams hashicorpNode;
  private final String azureKeyVault;
  private final Path multiKeySignerDirectory;

  public TransactionSignerParamsSupplier(
      final HashicorpSigningParams hashicorpNode,
      final String azureKeyVault,
      final Path multiKeySignerDirectory) {
    this.hashicorpNode = hashicorpNode;
    this.azureKeyVault = azureKeyVault;
    this.multiKeySignerDirectory = multiKeySignerDirectory;
  }

  public Collection<String> get() {
    final ArrayList<String> params = new ArrayList<>();
    if (hashicorpNode != null) {
      params.add("hashicorp-signer");
      params.add("--auth-file");
      params.add(createVaultAuthFile(hashicorpNode.getVaultToken()).getAbsolutePath());
      params.add("--host");
      params.add(hashicorpNode.getHost());
      params.add("--port");
      params.add(String.valueOf(hashicorpNode.getPort()));
      params.add("--signing-key-path");
      params.add(hashicorpNode.getSecretHttpPath());

      if (hashicorpNode.getServerCertificate().isEmpty()) {
        params.add("--tls-enabled=false");
      } else {
        final SelfSignedCertificate tlsCert = hashicorpNode.getServerCertificate().get();
        final Path trustStoreParentDir;
        try {
          trustStoreParentDir = Files.createTempDirectory("knownServerPath");
          final Path trustStorePath =
              createFingerprintFile(
                  trustStoreParentDir, tlsCert, Optional.of(hashicorpNode.getPort()));
          params.add("--tls-known-server-file");
          params.add(trustStorePath.toString());
        } catch (final IOException | CertificateEncodingException e) {
          throw new RuntimeException("Failed to construct hashicorp trust store file.");
        }
      }

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
    } else if (multiKeySignerDirectory != null) {
      params.add("multikey-signer");
      params.add("--directory");
      params.add(multiKeySignerDirectory.toAbsolutePath().toString());
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

  private File createVaultAuthFile(final String vaultToken) {
    return createTmpFile("vault_authfile", vaultToken.getBytes(UTF_8));
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

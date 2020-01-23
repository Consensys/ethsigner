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
package tech.pegasys.ethsigner.signer.hashicorp;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HashicorpTransactionSignerTest {

  @Test
  public void vaultTimingOut(@TempDir final Path tempDirectory) throws IOException {
    assertThatThrownBy(
            () ->
                HashicorpVaultSignerFactory.createSigner(
                    new HashicorpConfig.HashicorpConfigBuilder()
                        .withSigningKeyPath("signingKeyPath")
                        .withHost("serverHost")
                        .withPort(877)
                        .withAuthFilePath(createAuthFile(tempDirectory))
                        .withTimeout(Long.valueOf(1))
                        .build()))
        .isInstanceOf(TransactionSignerInitializationException.class);
  }

  @Test
  public void authFileNotAvailable() {

    assertThatThrownBy(
            () ->
                HashicorpVaultSignerFactory.createSigner(
                    new HashicorpConfig.HashicorpConfigBuilder()
                        .withSigningKeyPath("signingKeyPath")
                        .withHost("serverHost")
                        .withPort(877)
                        .withAuthFilePath(Path.of("nonExistingFile"))
                        .withTimeout(Long.valueOf(1))
                        .build()))
        .isInstanceOf(TransactionSignerInitializationException.class);
  }

  private Path createAuthFile(final Path tempDirectory) throws IOException {
    final Path tempFile = Files.createTempFile(tempDirectory, "file", ".file");
    Files.writeString(tempFile, "something");
    return tempFile;
  }
}

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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class HashicorpTransactionSignerTest {

  @Test
  public void vaultTimingOut() throws IOException {

    final File authFile = createFile();

    assertThatThrownBy(
            () ->
                HashicorpSignerFactory.createSigner(
                    "signingKeyPath", 877, "serverHost", authFile.toPath(), 1))
        .isInstanceOf(TransactionSignerInitializationException.class);
  }

  @Test
  public void authFileNotAvailable() {

    assertThatThrownBy(
            () ->
                HashicorpSignerFactory.createSigner(
                    "signingKeyPath", 877, "serverHost", Paths.get("nonExistingFile"), 1))
        .isInstanceOf(TransactionSignerInitializationException.class);
  }

  @SuppressWarnings("UnstableApiUsage")
  private static File createFile() throws IOException {
    final Path path = Files.createTempFile("file", ".file");
    Files.write(path, "something".getBytes(UTF_8));
    final File keyFile = path.toFile();
    keyFile.deleteOnExit();
    return keyFile;
  }
}

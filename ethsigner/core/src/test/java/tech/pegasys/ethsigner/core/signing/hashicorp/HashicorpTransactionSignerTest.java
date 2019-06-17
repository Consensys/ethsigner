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
package tech.pegasys.ethsigner.core.signing.hashicorp;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerInitializationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class HashicorpTransactionSignerTest {

  @Test(expected = TransactionSignerInitializationException.class)
  public void vaultTimingOut() throws IOException {

    final File authFile = createFile();

    final TransactionSigner signer =
        new HashicorpTransactionSigner(
            "signingKeyPath",
            Integer.valueOf(877),
            "serverHost",
            authFile.toPath(),
            Integer.valueOf(1));
  }

  @Test(expected = TransactionSignerInitializationException.class)
  public void authFileNotAvailable() {

    final TransactionSigner signer =
        new HashicorpTransactionSigner(
            "signingKeyPath",
            Integer.valueOf(877),
            "serverHost",
            Paths.get("nonExistingFile"),
            Integer.valueOf(1));
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

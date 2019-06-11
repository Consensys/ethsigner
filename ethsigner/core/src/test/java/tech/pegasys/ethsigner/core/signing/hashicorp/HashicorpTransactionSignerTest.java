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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerConfig;
import tech.pegasys.ethsigner.core.signing.TransactionSignerInitializationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class HashicorpTransactionSignerTest {

  @Test(expected = TransactionSignerInitializationException.class)
  public void vaultTimingOut() throws IOException {

    final TransactionSignerConfig configMock = mock(TransactionSignerConfig.class);

    final File authFile = createFile();

    final JsonObject jsonObject =
        new JsonObject()
            .put("authFilePath", authFile.getAbsolutePath())
            .put("signingKeyPath", "signingKeyPath")
            .put("serverPort", "877")
            .put("serverHost", "serverHost")
            .put("timeout", "1");

    when(configMock.jsonString()).thenReturn(jsonObject.encode());

    final TransactionSigner signer = new HashicorpTransactionSigner(configMock);
  }

  @Test(expected = TransactionSignerInitializationException.class)
  public void authFileNotAvailable() {

    final TransactionSignerConfig configMock = mock(TransactionSignerConfig.class);

    final JsonObject jsonObject =
        new JsonObject()
            .put("authFilePath", "nonExistingFile")
            .put("signingKeyPath", "signingKeyPath")
            .put("serverPort", "877")
            .put("serverHost", "serverHost")
            .put("timeout", "1");

    when(configMock.jsonString()).thenReturn(jsonObject.encode());

    final TransactionSigner signer = new HashicorpTransactionSigner(configMock);
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

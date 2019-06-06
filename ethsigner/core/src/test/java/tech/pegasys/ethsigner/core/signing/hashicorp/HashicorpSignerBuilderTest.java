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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.vertx.core.Vertx;
import org.junit.Test;

public class HashicorpSignerBuilderTest {

  private static final Vertx vertx = Vertx.vertx();

  @Test
  public void vaultTimingOut() throws IOException {

    final HashicorpSignerConfig configMock = mock(HashicorpSignerConfig.class);

    final File authFile = createFile();

    when(configMock.getAuthFilePath()).thenReturn(authFile.toPath());
    when(configMock.getSigningKeyPath()).thenReturn("signingKeyPath");
    when(configMock.getServerPort()).thenReturn(Integer.valueOf(877));
    when(configMock.getServerHost()).thenReturn("serverHost");
    when(configMock.getTimeout()).thenReturn(Integer.valueOf(1));

    final TransactionSigner signer = new HashicorpSignerBuilder(configMock, vertx).build();

    assertThat(signer).isNull();
  }

  @Test
  public void authFileNotAvailable() {

    final HashicorpSignerConfig configMock = mock(HashicorpSignerConfig.class);

    when(configMock.getAuthFilePath()).thenReturn(Paths.get("nonExistingFile"));

    final TransactionSigner signer = new HashicorpSignerBuilder(configMock, vertx).build();

    assertThat(signer).isNull();
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

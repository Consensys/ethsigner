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
package tech.pegasys.ethsigner.core.signing.fileBased;

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

import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.crypto.WalletUtils;

public class FileBasedSignerBuilderTest {

  private static String fileName;

  @BeforeClass
  public static void createKeyFile() {
    try {
      fileName = WalletUtils.generateFullNewWalletFile(MY_PASSWORD, null);
    } catch (final Exception e) {
      // intentionally empty
    }
    new File(fileName).deleteOnExit();
  }

  private static final String MY_PASSWORD = "myPassword";

  @Test
  public void success() throws IOException {
    final FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    final File keyFile = new File(fileName);
    final File pwdFile = createFile(MY_PASSWORD);

    when(configMock.getPasswordFilePath()).thenReturn(pwdFile.toPath());
    when(configMock.getKeyPath()).thenReturn(keyFile.toPath());

    final TransactionSigner signer = new FileBasedSignerBuilder(configMock).build();

    assertThat(signer).isNotNull();
  }

  @Test
  public void passwordInvalid() throws IOException {

    final FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    final File pwdFile = createFile("invalid");
    final File keyFile = new File(fileName);

    when(configMock.getPasswordFilePath()).thenReturn(pwdFile.toPath());
    when(configMock.getKeyPath()).thenReturn(keyFile.toPath());

    final TransactionSigner signer = new FileBasedSignerBuilder(configMock).build();

    assertThat(signer).isNull();
  }

  @Test
  public void passwordFileNotAvailable() {

    final FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    when(configMock.getPasswordFilePath()).thenReturn(Paths.get("nonExistingFile"));

    final TransactionSigner signer = new FileBasedSignerBuilder(configMock).build();

    assertThat(signer).isNull();
  }

  @Test
  public void keyFileNotAvailable() throws IOException {

    final FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    final File file = createFile("doesNotMatter");

    when(configMock.getPasswordFilePath()).thenReturn(file.toPath());
    when(configMock.getKeyPath()).thenReturn(Paths.get("nonExistingFile"));

    final TransactionSigner signer = new FileBasedSignerBuilder(configMock).build();

    assertThat(signer).isNull();
  }

  private static File createFile(final String s) throws IOException {
    final Path path = Files.createTempFile("file", ".file");
    Files.write(path, s.getBytes(UTF_8));
    final File file = path.toFile();
    file.deleteOnExit();
    return file;
  }
}

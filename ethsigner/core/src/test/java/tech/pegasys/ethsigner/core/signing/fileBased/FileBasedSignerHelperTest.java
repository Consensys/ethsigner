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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import tech.pegasys.ethsigner.core.signing.CredentialTransactionSigner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileBasedSignerHelperTest {

  private static final String KEY_FILE_STRING =
      "{\"address\":\"fe3b557e8fb62b89f4916b721be55ceb828dbd73\","
          + "\"crypto\":{\"cipher\":\"aes-128-ctr\","
          + "\"ciphertext\":\"c330870080626a6cf84c36eeb89f6376316637fbc16c86400eb6c394de5aed81\","
          + "\"cipherparams\":{\"iv\":\"7f2526feb38ac355c644a84e78ec5692\"},"
          + "\"kdf\":\"scrypt\","
          + "\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":"
          + "\"b0da609bb541df918eb55bbeaf237445aa406a0c2d6e975663c4fcb221bc54e0\"},"
          + "\"mac\":\"1ed0ef9e2092d0f5db8d16409ee2aaee9af3f521a5eafd910ce19dbc76e67ed7\"},"
          + "\"id\":\"43f8e00a-c6ec-4a62-b7cb-f54cb299c3db\",\"version\":3}";

  @Test
  public void testSuccess() throws IOException {
    FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    final File pwdFile = createFile("pass");
    final File keyFile = createFile(KEY_FILE_STRING);

    when(configMock.getPasswordFilePath()).thenReturn(pwdFile.toPath());
    when(configMock.getKeyPath()).thenReturn(keyFile.toPath());

    final CredentialTransactionSigner signer = FileBasedSignerHelper.getSigner(configMock);

    assertThat(signer).isNotNull();
  }

  @Test
  public void testPasswordInvalid() throws IOException {

    FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    final File pwdFile = createFile("invalid");
    final File keyFile = createFile(KEY_FILE_STRING);

    when(configMock.getPasswordFilePath()).thenReturn(pwdFile.toPath());
    when(configMock.getKeyPath()).thenReturn(keyFile.toPath());

    final CredentialTransactionSigner signer = FileBasedSignerHelper.getSigner(configMock);

    assertThat(signer).isNull();
  }

  @Test
  public void testPasswordFileNotAvailable() throws IOException {

    FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    when(configMock.getPasswordFilePath()).thenReturn(Paths.get("nonExistingFile"));

    final CredentialTransactionSigner signer = FileBasedSignerHelper.getSigner(configMock);

    assertThat(signer).isNull();
  }

  @Test
  public void testKeyFileNotAvailable() throws IOException {

    FileBasedSignerConfig configMock = mock(FileBasedSignerConfig.class);

    final File file = createFile("doesNotMatter");

    when(configMock.getPasswordFilePath()).thenReturn(file.toPath());
    when(configMock.getKeyPath()).thenReturn(Paths.get("nonExistingFile"));

    final CredentialTransactionSigner signer = FileBasedSignerHelper.getSigner(configMock);

    assertThat(signer).isNull();
  }

  @SuppressWarnings("UnstableApiUsage")
  private static File createFile(String s) throws IOException {
    final Path path = Files.createTempFile("file", ".file");
    Files.write(path, s.getBytes(UTF_8));
    File file = path.toFile();
    file.deleteOnExit();
    return file;
  }
}

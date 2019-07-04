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
package tech.pegasys.ethsigner.signer.filebased;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.Signature;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.io.BaseEncoding;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

public class FileBasedTransactionSignerTest {

  private static final String INVALID_PASSWORD = "invalid";
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
    final File keyFile = new File(fileName);
    final File pwdFile = createFile(MY_PASSWORD);

    final TransactionSigner signer =
        FileBasedSignerFactory.createSigner(keyFile.toPath(), pwdFile.toPath());

    assertThat(signer).isNotNull();
    assertThat(signer.getAddress()).isNotEmpty();
  }

  @Test
  public void passwordInvalid() throws IOException {

    final File pwdFile = createFile(INVALID_PASSWORD);
    final File keyFile = new File(fileName);

    assertThatThrownBy(
            () -> FileBasedSignerFactory.createSigner(keyFile.toPath(), pwdFile.toPath()))
        .isInstanceOf(TransactionSignerInitializationException.class);
  }

  @Test
  public void passwordFileNotAvailable() {

    final File keyFile = new File(fileName);

    assertThatThrownBy(
            () ->
                FileBasedSignerFactory.createSigner(keyFile.toPath(), Paths.get("nonExistingFile")))
        .isInstanceOf(TransactionSignerInitializationException.class);
  }

  @Test
  public void keyFileNotAvailable() throws IOException {

    final File pwdFile = createFile(MY_PASSWORD);

    assertThatThrownBy(
            () ->
                FileBasedSignerFactory.createSigner(Paths.get("nonExistingFile"), pwdFile.toPath()))
        .isInstanceOf(TransactionSignerInitializationException.class);
  }

  private static File createFile(final String s) throws IOException {
    final Path path = Files.createTempFile("file", ".file");
    Files.write(path, s.getBytes(UTF_8));

    final File file = path.toFile();
    file.deleteOnExit();
    return file;
  }

  @Test
  public void web3jSigningOutputs() {
    final String privKeyStr =
        "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63".toUpperCase();

    final BigInteger privKey = new BigInteger(1, BaseEncoding.base16().decode(privKeyStr));
    final ECKeyPair keyPair = ECKeyPair.create(privKey);

    Credentials creds = Credentials.create(keyPair);

    CredentialTransactionSigner signer = new CredentialTransactionSigner(creds);

    byte[] data = {1, 2, 3};
    final Signature signature1 = signer.sign(data);
    final Signature signature2 = signer.sign(data);
    final Signature signature3 = signer.sign(data);

    assertThat(signature1).isEqualToComparingFieldByField(signature2);
    assertThat(signature2).isEqualToComparingFieldByField(signature3);
  }
}

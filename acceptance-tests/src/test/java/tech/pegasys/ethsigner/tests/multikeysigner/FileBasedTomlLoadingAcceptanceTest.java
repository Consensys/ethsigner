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
package tech.pegasys.ethsigner.tests.multikeysigner;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.FAILED_TO_CONNECT_TO_DOWNSTREAM_NODE;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.protocol.core.methods.request.Transaction;

class FileBasedTomlLoadingAcceptanceTest extends MultiKeyAcceptanceTestBase {

  static final String FILENAME = "a01f618424b0113a9cebdc6cb66ca5b48e9120c5";
  static final String FILE_ETHEREUM_ADDRESS = "0x" + FILENAME;

  @Test
  void validFileBasedTomlFileProducesSignerWhicReportsMatchingAddress(@TempDir Path tomlDirectory)
      throws URISyntaxException {
    createFileBasedTomlFileAt(
        tomlDirectory.resolve("arbitrary_prefix" + FILENAME + ".toml").toAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath());

    setup(tomlDirectory);

    assertThat(ethSigner.accounts().list()).containsOnly(FILE_ETHEREUM_ADDRESS);
  }

  @Test
  void validFileBasedTomlFileWithMultineLinePasswordFileProducesSignerWhichReportsMatchingAddress(
      @TempDir Path tomlDirectory) throws URISyntaxException, IOException {
    final Path passwordFile =
        Files.writeString(
            tomlDirectory.resolve("password.txt"), String.format("password%nsecond line%n"));
    createFileBasedTomlFileAt(
        tomlDirectory.resolve("arbitrary_prefix" + FILENAME + ".toml").toAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        passwordFile.toString());

    setup(tomlDirectory);

    assertThat(ethSigner.accounts().list()).containsOnly(FILE_ETHEREUM_ADDRESS);
  }

  @Test
  void incorrectlyNamedFileBasedSignerIsNotLoaded(@TempDir Path tomlDirectory)
      throws URISyntaxException {
    createFileBasedTomlFileAt(
        tomlDirectory.resolve("ffffffffffffffffffffffffffffffffffffffff.toml").toAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath());

    setup(tomlDirectory);

    assertThat(ethSigner.accounts().list()).isEmpty();
  }

  @Test
  void newConfigFilesAreLoadedForSendTransaction(@TempDir Path tomlDirectory) throws Exception {
    createFileBasedTomlFileAt(
        tomlDirectory.resolve(FileBasedTomlLoadingAcceptanceTest.FILENAME + ".toml"),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath());

    setup(tomlDirectory);

    // As besu is not started for this test, the available account would result in failed to connect
    // error instead of locked account error.
    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        sendTransaction("a01f618424b0113a9cebdc6cb66ca5b48e9120c5");
    assertThat(signerResponse.status()).isEqualTo(GATEWAY_TIMEOUT);
    assertThat(signerResponse.jsonRpc().getError()).isEqualTo(FAILED_TO_CONNECT_TO_DOWNSTREAM_NODE);

    final String additionalAddress = "0cd9595a25811ce48d1fb8c5221d1193e187c363";

    // account not loaded via config file should raise locked account error
    final SignerResponse<JsonRpcErrorResponse> lockedAccountResponse =
        sendTransaction(additionalAddress);
    assertThat(lockedAccountResponse.status()).isEqualTo(BAD_REQUEST);
    assertThat(lockedAccountResponse.jsonRpc().getError())
        .isEqualTo(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);

    // add config file for additional address
    createFileBasedTomlFileAt(
        tomlDirectory.resolve(additionalAddress + ".toml"),
        new File(Resources.getResource(additionalAddress + ".key").toURI()).getAbsolutePath(),
        new File(Resources.getResource(additionalAddress + ".password").toURI()).getAbsolutePath());

    // we should now get failed to connect exception instead of locked account.
    final SignerResponse<JsonRpcErrorResponse> newConfigSignerResponse =
        sendTransaction(additionalAddress);
    assertThat(newConfigSignerResponse.status()).isEqualTo(GATEWAY_TIMEOUT);
    assertThat(newConfigSignerResponse.jsonRpc().getError())
        .isEqualTo(FAILED_TO_CONNECT_TO_DOWNSTREAM_NODE);
  }

  private SignerResponse<JsonRpcErrorResponse> sendTransaction(final String address) {
    final Account sender = new Account("0x" + address);
    final String recipientAddress = "0x1b22ba22ca22bb22aa22bc22be22ac22ca22da22";

    final Transaction transaction =
        Transaction.createEtherTransaction(
            sender.address(),
            sender.nextNonce(),
            GAS_PRICE,
            INTRINSIC_GAS,
            recipientAddress,
            BigInteger.ONE);

    return ethSigner.transactions().submitExceptional(transaction);
  }
}

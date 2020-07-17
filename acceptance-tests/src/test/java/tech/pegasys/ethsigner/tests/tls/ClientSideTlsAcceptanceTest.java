/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.tests.tls;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;
import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.populateFingerprintFile;

import tech.pegasys.ethsigner.core.config.KeyStoreOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodeConfig;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;
import tech.pegasys.ethsigner.tests.tls.support.MockBalanceReporter;
import tech.pegasys.ethsigner.tests.tls.support.TlsEnabledHttpServerFactory;
import tech.pegasys.ethsigner.tests.tls.support.client.BasicClientTlsOptions;
import tech.pegasys.ethsigner.tests.tls.support.client.BasicKeyStoreOptions;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;

import io.vertx.core.http.HttpServer;
import org.bouncycastle.util.Integers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

class ClientSideTlsAcceptanceTest {

  private TlsEnabledHttpServerFactory serverFactory;
  private Signer signer;
  private static final int UNUSED_WS_PORT = 0;

  @BeforeEach
  void setup() {
    serverFactory = new TlsEnabledHttpServerFactory();
  }

  @AfterEach
  void cleanup() {
    serverFactory.shutdown();
    if (signer != null) {
      signer.shutdown();
      signer = null;
    }
  }

  private Signer createAndStartSigner(
      final TlsCertificateDefinition presentedCert,
      final TlsCertificateDefinition expectedWeb3ProviderCert,
      final int downstreamWeb3Port,
      final int listenPort,
      final Path workDir)
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
    final Signer signer =
        createSigner(
            presentedCert, expectedWeb3ProviderCert, downstreamWeb3Port, listenPort, workDir);
    signer.start();
    signer.awaitStartupCompletion();

    return signer;
  }

  private Signer createSigner(
      final TlsCertificateDefinition presentedCert,
      final TlsCertificateDefinition expectedWeb3ProviderCert,
      final int downstreamWeb3Port,
      final int listenPort,
      final Path workDir)
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {

    final Path clientPasswordFile =
        Files.writeString(workDir.resolve("clientKeystorePassword"), presentedCert.getPassword());

    final Path fingerPrintFilePath = workDir.resolve("known_servers");
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    final Optional<Integer> downstreamWeb3ServerPort =
        Optional.of(Integers.valueOf(downstreamWeb3Port));

    populateFingerprintFile(
        fingerPrintFilePath, expectedWeb3ProviderCert, downstreamWeb3ServerPort);

    final KeyStoreOptions keyStoreOptions =
        new BasicKeyStoreOptions(presentedCert.getPkcs12File().toPath(), clientPasswordFile);
    final ClientTlsOptions clientTlsOptions =
        new BasicClientTlsOptions(keyStoreOptions, Optional.of(fingerPrintFilePath), true);
    builder.withDownstreamTlsOptions(clientTlsOptions);

    builder.withHttpRpcPort(listenPort);

    final BesuNodePorts besuNodePorts = new BesuNodePorts(downstreamWeb3Port, UNUSED_WS_PORT);

    signer = new Signer(builder.build(), BesuNodeConfig.DEFAULT_HOST, besuNodePorts);

    return signer;
  }

  @Test
  void ethSignerProvidesSpecifiedClientCertificateToDownStreamServer(@TempDir Path workDir)
      throws Exception {

    final TlsCertificateDefinition serverCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert2.pfx", "password2");

    // Note: the HttpServer always responds with a JsonRpcSuccess, result=300.
    final HttpServer web3ProviderHttpServer =
        serverFactory.create(serverCert, ethSignerCert, workDir);

    signer =
        createAndStartSigner(
            ethSignerCert, serverCert, web3ProviderHttpServer.actualPort(), 0, workDir);

    assertThat(signer.accounts().balance("0x123456"))
        .isEqualTo(BigInteger.valueOf(MockBalanceReporter.REPORTED_BALANCE));
  }

  @Test
  void ethSignerDoesNotConnectToServerNotSpecifiedInTrustStore(@TempDir Path workDir)
      throws Exception {
    final TlsCertificateDefinition serverPresentedCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert2.pfx", "password2");
    final TlsCertificateDefinition ethSignerExpectedServerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert2.pfx", "password2");

    final HttpServer web3ProviderHttpServer =
        serverFactory.create(serverPresentedCert, ethSignerCert, workDir);

    signer =
        createAndStartSigner(
            ethSignerCert,
            ethSignerExpectedServerCert,
            web3ProviderHttpServer.actualPort(),
            0,
            workDir);

    assertThatThrownBy(() -> signer.accounts().balance("0x123456"))
        .isInstanceOf(ClientConnectionException.class)
        .hasMessageContaining(String.valueOf(BAD_GATEWAY.code()));

    // ensure submitting a transaction results in the same behaviour
    final Transaction transaction =
        Transaction.createEtherTransaction(
            signer.accounts().richBenefactor().address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00",
            Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact());

    assertThatThrownBy(() -> signer.transactions().submit(transaction))
        .isInstanceOf(ClientConnectionException.class)
        .hasMessageContaining(String.valueOf(BAD_GATEWAY.code()));
  }

  @Test
  void missingKeyStoreForEthSignerResultsInEthSignerTerminating(@TempDir Path workDir)
      throws Exception {
    final TlsCertificateDefinition serverPresentedCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        new TlsCertificateDefinition(
            workDir.resolve("Missing_keyStore").toFile(), "arbitraryPassword");

    // Ports are arbitrary as EthSigner should exit
    signer = createSigner(ethSignerCert, serverPresentedCert, 9000, 9001, workDir);
    signer.start();
    waitFor(() -> assertThat(signer.isRunning()).isFalse());
  }

  @Test
  void incorrectPasswordForDownstreamKeyStoreResultsInEthSignerTerminating(@TempDir Path workDir)
      throws Exception {
    final TlsCertificateDefinition serverPresentedCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "wrong_password");

    // Ports are arbitrary as EthSigner should exit
    signer = createSigner(ethSignerCert, serverPresentedCert, 9000, 9001, workDir);
    signer.start();
    waitFor(() -> assertThat(signer.isRunning()).isFalse());
  }
}

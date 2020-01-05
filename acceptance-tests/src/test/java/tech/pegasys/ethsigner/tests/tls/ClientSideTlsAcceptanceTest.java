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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;
import static tech.pegasys.ethsigner.tests.tls.support.TlsEnabledHttpServer.createServer;

import io.vertx.core.http.HttpServer;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;
import tech.pegasys.ethsigner.tests.tls.support.BasicPkcsStoreConfig;
import tech.pegasys.ethsigner.tests.tls.support.MockBalanceReporter;

class ClientSideTlsAcceptanceTest {

  private Signer createAndStartSigner(
      final TlsCertificateDefinition presentedCert,
      final TlsCertificateDefinition expectedWeb3ProviderCert,
      final int downstreamWeb3Port,
      final int listenPort,
      final Path workDir)
      throws IOException {
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
      throws IOException {

    // Create an EthSigner
    final Path clientPasswordFile =
        Files.writeString(workDir.resolve("clientKeystorePassword"), presentedCert.getPassword());
    final Path serverPasswordFile =
        Files.writeString(
            workDir.resolve("clientTrustStorePassword"), expectedWeb3ProviderCert.getPassword());

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withDownstreamTrustStore(
        new BasicPkcsStoreConfig(
            expectedWeb3ProviderCert.getPkcs12File(), serverPasswordFile.toFile()));
    builder.withDownstreamKeyStore(
        new BasicPkcsStoreConfig(presentedCert.getPkcs12File(), clientPasswordFile.toFile()));
    builder.withHttpRpcPort(listenPort);

    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();
    final NodePorts nodePorts = new NodePorts(downstreamWeb3Port, 9);

    final Signer signer = new Signer(builder.build(), nodeConfig, nodePorts);

    return signer;
  }

  @Test
  void ethSignerProvidesSpecifiedClientCertificateToDownStreamServer(@TempDir Path workDir)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

    final TlsCertificateDefinition serverCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert2.pfx", "password2");

    // Note: the HttpServer always responds with a JsonRpcSuccess, result=300.
    final HttpServer web3ProviderHttpServer = createServer(serverCert, ethSignerCert, workDir);

    final Signer signer =
        createAndStartSigner(
            ethSignerCert, serverCert, web3ProviderHttpServer.actualPort(), 0, workDir);

    assertThat(signer.accounts().balance("0x123456"))
        .isEqualTo(BigInteger.valueOf(MockBalanceReporter.REPORTED_BALANCE));
  }

  @Test
  void ethSignerDoesNotConnectToServerNotSpecifiedInTrustStore(@TempDir Path workDir)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    final TlsCertificateDefinition serverPresentedCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert2.pfx", "password2");
    final TlsCertificateDefinition ethSignerExpectedServerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert2.pfx", "password2");

    final HttpServer web3ProviderHttpServer =
        createServer(serverPresentedCert, ethSignerCert, workDir);

    final Signer signer =
        createAndStartSigner(
            ethSignerCert,
            ethSignerExpectedServerCert,
            web3ProviderHttpServer.actualPort(),
            0,
            workDir);

    assertThatThrownBy(() -> signer.accounts().balance("0x123456"))
        .isInstanceOf(ClientConnectionException.class)
        .hasMessageContaining("500");


    final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";
    final BigInteger transferAmountWei = Convert.toWei("1.75", Unit.ETHER).toBigIntegerExact();
    final Transaction transaction =
        Transaction.createEtherTransaction(
            signer.accounts().richBenefactor().address(),
            null,
            GAS_PRICE,
            INTRINSIC_GAS,
            RECIPIENT,
            transferAmountWei);

    signer.transactions().submit(transaction);

  }

  @Test
  void missingKeyStoreForEthSignerResultsTerminatesEthSigner(@TempDir Path workDir)
      throws IOException {
    final TlsCertificateDefinition serverPresentedCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        new TlsCertificateDefinition(
            workDir.resolve("Missing_keyStore").toFile(), "arbitraryPassword");

    // Ports are arbitrary as EthSigner should exit
    final Signer signer = createSigner(ethSignerCert, serverPresentedCert, 9000, 9001, workDir);
    signer.start();
    waitFor(() -> assertThat(signer.isRunning()).isFalse());
  }

  @Test
  void incorrectPasswordForWeb3ProviderKeyStroreResultsInEthSignerTerminating(@TempDir Path workDir)
      throws IOException {
    final TlsCertificateDefinition serverPresentedCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
    final TlsCertificateDefinition ethSignerCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "wrong_password");

    // Ports are arbitrary as EthSigner should exit
    final Signer signer = createSigner(ethSignerCert, serverPresentedCert, 9000, 9001, workDir);
    signer.start();
    waitFor(() -> assertThat(signer.isRunning()).isFalse());
  }
/*
  @Test
  void transmittingATransactionWithoutNonceToAServerWithMismatchedCertificatesProvidesBadGateway(
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

  }
 */
}

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
package tech.pegasys.ethsigner.tests.tls;

import static java.nio.file.Files.writeString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static tech.pegasys.ethsigner.tests.WaitUtils.waitFor;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.pegasys.ethsigner.core.TlsOptions;
import tech.pegasys.ethsigner.tests.dsl.ClientConfig;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.tls.BasicTlsOptions;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;

public class ServerSideTlsAcceptanceTest {

  // To create a PKCS12 keystore file (containing a privKey and Certificate:
  // Read
  // https://www.digitalocean.com/community/tutorials/openssl-essentials-working-with-ssl-certificates-private-keys-and-csrs
  // To make this simpler, have a config file, containing:
  //  [req]
  //  distinguished_name = req_distinguished_name
  //  x509_extensions = v3_req
  //  prompt = no
  //  [req_distinguished_name]
  //  C = AU
  //  ST = QLD
  //  L = Brisbane
  //  O = PegaSys
  //  OU = Prod Dev
  //  CN = localhost
  //  [v3_req]
  //  keyUsage = keyEncipherment, dataEncipherment
  //  extendedKeyUsage = serverAuth
  //  subjectAltName = @alt_names
  //  [alt_names]
  //  DNS.1 = localhost
  //  IP.1 = 127.0.0.1
  // 1. Create a CSR and private key
  // CMD    = openssl req -newkey rsa:2048 -nodes -keyout domain.key -out domain.csr -config conf
  // OUTPUT = domain.csr, and domain.key in the current working directory
  // 2. Generate self-signed certificate
  // CMD    = openssl req -key domain.key -new -x509 -days 365 -out cert.crt -config conf
  // OUTPUT = cert.crt in the current working directory
  // 3. Convert to PKCS12
  // openssl pkcs12 -export -inkey domain.key -in cert.crt -out cert.pfx
  //
  @TempDir
  Path dataPath;

  final TlsCertificateDefinition cert1 =
      TlsCertificateDefinition.loadFromResource("tls/cert1", "password");
  final TlsCertificateDefinition cert2 =
      TlsCertificateDefinition.loadFromResource("tls/cert2", "password2");

  private Signer createTlsEthSigner(final TlsCertificateDefinition serverPresentedCerts,
      final TlsCertificateDefinition clientExpectedCert,
      final int fixedListenPort) {

    try {
      /*
      final Path fingerPrintFile = dataPath.resolve("known_clients");
      generateClientFingerPrint(fingerPrintFile.toAbsolutePath(), certificateFile);
       */

      final Path passwordPath = dataPath.resolve("keystore.passwd");
      if (serverPresentedCerts.getPassword() != null) {
        writeString(passwordPath, serverPresentedCerts.getPassword());
      }

      final TlsOptions serverOptions =
          new BasicTlsOptions(serverPresentedCerts.getPkcs12File(), passwordPath.toFile(),
              Optional.empty());

      final SignerConfigurationBuilder configBuilder =
          new SignerConfigurationBuilder().withServerTlsOptions(serverOptions)
              .withHttpRpcPort(fixedListenPort);

      final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();

      final ClientConfig clientConfig =
          new ClientConfig(clientExpectedCert.getCertificateFile(), null);

      final Signer ethSigner =
          new Signer(configBuilder.build(), nodeConfig, new NodePorts(1, 2), clientConfig);

      return ethSigner;
    } catch (final Exception e) {
      fail("Failed to create EthSigner.", e);
      return null;
    }
  }

  @Test
  void ableToConnectWhenClientExpectsSameCertificateAsThatPresented() {
    final Signer ethSigner = createTlsEthSigner(cert1, cert1, 0);
    ethSigner.start();
    ethSigner.awaitStartupCompletion();

    assertThat(ethSigner.accounts().list()).isNotEmpty();
  }
/*
  @Test
  void nonTlsClientsCannotConnectToTlsEnabledEthSigner() {
    // The ethSigner object (and in-built requester are already TLS enabled, so need to make a new
    // http client which does not have TLS enabled
    final Signer ethSigner = createTlsEthSigner(cert1, );
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
    final HttpRequest rawRequests =
        new HttpRequest(
            ethSigner.getUrlEndpoint(), OkHttpClientHelpers.createOkHttpClient(Optional.empty()));

    final Throwable thrown = catchThrowable(() -> rawRequests.get("/upcheck"));

    assertThat(thrown.getCause()).isInstanceOf(SSLHandshakeException.class);
  }
 */

  @Test
  void missingPasswordFileResultsInEthsignerExiting() {
    //arbitrary port to prevent waiting for portfile (during Start) to be created.
    final TlsCertificateDefinition missingPasswordCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1", null);
    final Signer ethSigner = createTlsEthSigner(missingPasswordCert, cert1, 9000);
    ethSigner.start();
    waitFor(() -> assertThat(ethSigner.isRunning()).isFalse());
  }


  @Test
  void ethSignerExitsIfPasswordDoesntMatchKeyStoreFile() {
    //arbitrary port to prevent waiting for portfile (during Start) to be created.
    final TlsCertificateDefinition wrongPasswordCert =
        TlsCertificateDefinition.loadFromResource("tls/cert1", "wrongPassword");
    final Signer ethSigner = createTlsEthSigner(wrongPasswordCert, cert1, 9000);
    ethSigner.start();
    waitFor(() -> assertThat(ethSigner.isRunning()).isFalse());
  }
  /*

  @Test
  void missingKeyStoreFileResultsInEthsignerExiting() {

  }

  @Test
  void clientMissingFromWhiteListCannotConnectToEthSigner() {

  }
 */
}

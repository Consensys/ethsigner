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
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import tech.pegasys.ethsigner.core.TlsOptions;
import tech.pegasys.ethsigner.tests.dsl.http.HttpRequest;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.tls.OkHttpClientHelpers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import javax.net.ssl.SSLHandshakeException;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
  // CMD    =openssl req -newkey rsa:2048 -nodes -keyout domain.key -out domain.csr -config conf
  // OUTPUT = domain.csr, and domain.key in the current working directory
  // 2. Generate self-signed certificate
  // CMD    = openssl req -key domain.key -new -x509 -days 365 -out domain.crt -config conf
  // OUTPUT = domain.crt in the current working directory
  // 3. Convert to PKCS12
  // openssl pkcs12 -export -inkey domain.key -in domain.crt -out domain.pfx
  //
  private static Signer ethSigner;

  @TempDir Path dataPath;

  @BeforeEach
  public void setup() throws IOException, URISyntaxException {
    final URL sslCertificate = Resources.getResource("domain.pfx");
    final Path certPath = Path.of(sslCertificate.getPath());

    final File expectedServerTlsCert =
        Path.of(Resources.getResource("domain.crt").toURI()).toFile();

    final Path passwordPath = writeString(dataPath.resolve("keystore.passwd"), "password");

    final TlsOptions serverOptions =
        new TlsOptions() {

          @Override
          public File getKeyStoreFile() {
            return certPath.toFile();
          }

          @Override
          public File getKeyStorePasswordFile() {
            return passwordPath.toFile();
          }

          @Override
          public Optional<File> getKnownClientsFile() {
            return Optional.empty();
          }
        };

    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withServerTlsOptions(serverOptions).build();
    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();

    ethSigner = new Signer(signerConfig, nodeConfig, new NodePorts(1, 2), expectedServerTlsCert);
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  @Test
  void ableToConnect() {
    assertThat(ethSigner.accounts().list()).isNotEmpty();
  }

  @Test
  void nonTlsClientsCannotConnectToTlsEnabledEthSigner() {
    // The ethSigner object (and in-built requester are already TLS enabled, so need to make a new
    // http client which does not have TLS enabled
    final HttpRequest rawRequests =
        new HttpRequest(
            ethSigner.getUrlEndpoint(), OkHttpClientHelpers.createOkHttpClient(Optional.empty()));

    final Throwable thrown = catchThrowable(() -> rawRequests.get("/upcheck"));

    assertThat(thrown.getCause()).isInstanceOf(SSLHandshakeException.class);
  }
}

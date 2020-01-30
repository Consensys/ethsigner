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
package tech.pegasys.ethsigner;

import tech.pegasys.ethsigner.core.config.Config;
import tech.pegasys.ethsigner.core.config.PkcsStoreConfig;
import tech.pegasys.ethsigner.core.config.TlsOptions;
import tech.pegasys.ethsigner.core.signing.ChainIdProvider;
import tech.pegasys.ethsigner.core.signing.ConfigurationChainId;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Level;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@SuppressWarnings("FieldCanBeLocal") // because Picocli injected fields report false positives
@Command(
    description =
        "This command runs the EthSigner.\n"
            + "Documentation can be found at https://docs.ethsigner.pegasys.tech.",
    abbreviateSynopsis = true,
    name = "ethsigner",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    subcommands = {HelpCommand.class},
    footer = "EthSigner is licensed under the Apache License 2.0")
public class EthSignerBaseCommand implements Config {

  static class TlsClientCertificateOptions implements PkcsStoreConfig {

    @Option(
        names = "--downstream-http-tls-keystore-file",
        description =
            "Path to a PKCS#12 formatted keystore, contains TLS certificate to present to "
                + "a TLS-enabled web3 provider",
        arity = "1",
        required = true)
    private File clientCertificateFile;

    @Option(
        names = "--downstream-http-tls-keystore-password-file",
        description = "Path to a file containing the password used to decrypt the client cert.",
        arity = "1",
        required = true)
    private File clientCertificatePasswordFile;

    @Override
    public File getStoreFile() {
      return clientCertificateFile;
    }

    @Override
    public File getStorePasswordFile() {
      return clientCertificatePasswordFile;
    }
  }

  static class TlsClientAuthentication {
    @Option(
        names = "--tls-known-clients-file",
        description = "Path to a file containing the fingerprints of authorized clients.",
        arity = "1",
        required = true)
    private File tlsKnownClientsFile = null;

    @SuppressWarnings("UnusedVariable")
    @Option(
        names = "--tls-allow-any-client",
        description =
            "If defined, will allow any client to connect. Is mutually exclusive with "
                + "--tls-known-clients-file.",
        arity = "0",
        required = false)
    private Boolean tlsAllowAnyClient = false;
  }

  static class TlsServerOptions implements TlsOptions {

    @Option(
        names = "--tls-keystore-file",
        description =
            "Path to a PKCS#12 formatted keystore; used to enable TLS on inbound connections.",
        arity = "1",
        required = true)
    private File keyStoreFile;

    @Option(
        names = "--tls-keystore-password-file",
        description = "Path to a file containing the password used to decrypt the keystore.",
        arity = "1",
        required = true)
    private File keyStorePasswordFile;

    @ArgGroup(multiplicity = "1")
    private TlsClientAuthentication tlsClientAuthentication;

    @Override
    public File getKeyStoreFile() {
      return keyStoreFile;
    }

    @Override
    public File getKeyStorePasswordFile() {
      return keyStorePasswordFile;
    }

    @Override
    public Optional<File> getKnownClientsFile() {
      return Optional.ofNullable(tlsClientAuthentication.tlsKnownClientsFile);
    }
  }

  @Option(
      names = "--downstream-http-tls-known-servers-file",
      description = "Path to a file containing the fingerprints of authorized servers",
      arity = "1",
      required = false)
  private File downstreamKnownServersFile;

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = Level.INFO;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = "--downstream-http-host",
      description =
          "The endpoint to which received requests are forwarded (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String downstreamHttpHost = InetAddress.getLoopbackAddress().getHostAddress();

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = "--downstream-http-port",
      description = "The endpoint to which received requests are forwarded",
      required = true,
      arity = "1")
  private Integer downstreamHttpPort;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--downstream-http-request-timeout"},
      description =
          "Timeout in milliseconds to wait for downstream request (default: ${DEFAULT-VALUE})",
      arity = "1")
  private long downstreamHttpRequestTimeout = Duration.ofSeconds(5).toMillis();

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--http-listen-host"},
      description = "Host for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String httpListenHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--http-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Integer httpListenPort = 8545;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--chain-id"},
      description = "The Chain Id that will be the intended recipient for signed transactions",
      required = true,
      arity = "1")
  private long chainId;

  @Option(
      names = {"--data-path"},
      description = "The path to a directory to store temporary files",
      arity = "1")
  private Path dataPath;

  @ArgGroup(exclusive = false)
  private TlsServerOptions tlsServerOptions;

  @ArgGroup(exclusive = false)
  private TlsClientCertificateOptions clientTlsCertificateOptions;

  @Override
  public Level getLogLevel() {
    return logLevel;
  }

  @Override
  public String getDownstreamHttpHost() {
    return downstreamHttpHost;
  }

  @Override
  public Integer getDownstreamHttpPort() {
    return downstreamHttpPort;
  }

  @Override
  public String getHttpListenHost() {
    return httpListenHost;
  }

  @Override
  public Integer getHttpListenPort() {
    return httpListenPort;
  }

  @Override
  public ChainIdProvider getChainId() {
    return new ConfigurationChainId(chainId);
  }

  @Override
  public Path getDataPath() {
    return dataPath;
  }

  @Override
  public Duration getDownstreamHttpRequestTimeout() {
    return Duration.ofMillis(downstreamHttpRequestTimeout);
  }

  @Override
  public Optional<TlsOptions> getTlsOptions() {
    return Optional.ofNullable(tlsServerOptions);
  }

  @Override
  public Optional<PkcsStoreConfig> getClientCertificateOptions() {
    return Optional.ofNullable(clientTlsCertificateOptions);
  }

  @Override
  public Optional<File> getWeb3ProviderKnownServersFile() {
    return Optional.ofNullable(downstreamKnownServersFile);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("logLevel", logLevel)
        .add("downstreamHttpHost", downstreamHttpHost)
        .add("downstreamHttpPort", downstreamHttpPort)
        .add("downstreamHttpRequestTimeout", downstreamHttpRequestTimeout)
        .add("httpListenHost", httpListenHost)
        .add("httpListenPort", httpListenPort)
        .add("chainId", chainId)
        .add("dataPath", dataPath)
        .toString();
  }
}

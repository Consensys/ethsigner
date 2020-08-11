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

import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_HOST_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_LONG_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_PORT_FORMAT_HELP;

import tech.pegasys.ethsigner.config.ConfigFileOption;
import tech.pegasys.ethsigner.config.InvalidCommandLineOptionsException;
import tech.pegasys.ethsigner.config.PicoCliTlsServerOptions;
import tech.pegasys.ethsigner.config.tls.client.PicoCliClientTlsOptions;
import tech.pegasys.ethsigner.core.CorsAllowedOriginsProperty;
import tech.pegasys.ethsigner.core.config.Config;
import tech.pegasys.ethsigner.core.config.TlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.signing.ChainIdProvider;
import tech.pegasys.ethsigner.core.signing.ConfigurationChainId;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Level;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@SuppressWarnings("FieldCanBeLocal") // because Picocli injected fields report false positives
@Command(
    description =
        "This command runs the EthSigner.\n"
            + "Documentation can be found at https://docs.ethsigner.pegasys.tech.",
    abbreviateSynopsis = true,
    name = "ethsigner",
    sortOptions = false,
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    synopsisSubcommandLabel = "COMMAND",
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    subcommands = {HelpCommand.class},
    footer = "EthSigner is licensed under the Apache License 2.0")
public class EthSignerBaseCommand implements Config, Runnable {

  @Spec private CommandSpec spec; // injected by picocli

  @SuppressWarnings("UnusedVariable")
  @Mixin
  private ConfigFileOption configFileOption;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--chain-id"},
      description = "The Chain Id that will be the intended recipient for signed transactions",
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      arity = "1")
  private Long chainId; // required, see validateArgs

  @Option(
      names = {"--data-path"},
      description = "The path to a directory to store temporary files",
      paramLabel = MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  private Path dataPath;

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = Level.INFO;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--http-listen-host"},
      description = "Host for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String httpListenHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--http-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      arity = "1")
  private final Integer httpListenPort = 8545;

  @Mixin private PicoCliTlsServerOptions picoCliTlsServerOptions;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = "--downstream-http-host",
      description =
          "The endpoint to which received requests are forwarded (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String downstreamHttpHost = InetAddress.getLoopbackAddress().getHostAddress();

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = "--downstream-http-port",
      description = "The endpoint to which received requests are forwarded",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      arity = "1")
  private Integer downstreamHttpPort; // required, see validateArgs

  private String downstreamHttpPath = "/";

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--downstream-http-path"},
      description = "The path to which received requests are forwarded (default: ${DEFAULT-VALUE})",
      defaultValue = "/",
      paramLabel = MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  public void setDownstreamHttpPath(final String path) {
    try {
      final URI uri = new URI(path);
      if (!uri.getPath().equals(path)) {
        throw new ParameterException(
            spec.commandLine(), "Illegal characters detected in --downstream-http-path");
      }
    } catch (final URISyntaxException e) {
      throw new ParameterException(
          spec.commandLine(), "Illegal characters detected in --downstream-http-path");
    }
    this.downstreamHttpPath = path;
  }

  // A list of origins URLs that are accepted by the JsonRpcHttpServer (CORS)
  @Option(
      names = {"--http-cors-origins"},
      description = "Comma separated origin domain URLs for CORS validation (default: none)")
  private final CorsAllowedOriginsProperty rpcHttpCorsAllowedOrigins =
      new CorsAllowedOriginsProperty();

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--downstream-http-request-timeout"},
      description =
          "Timeout in milliseconds to wait for downstream request (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      arity = "1")
  private long downstreamHttpRequestTimeout = Duration.ofSeconds(5).toMillis();

  @Mixin private PicoCliClientTlsOptions clientTlsOptions;

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
  public String getDownstreamHttpPath() {
    return downstreamHttpPath;
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
    return picoCliTlsServerOptions.isTlsEnabled()
        ? Optional.of(picoCliTlsServerOptions)
        : Optional.empty();
  }

  @Override
  public Optional<ClientTlsOptions> getClientTlsOptions() {
    return clientTlsOptions.isTlsEnabled() ? Optional.of(clientTlsOptions) : Optional.empty();
  }

  @Override
  public Collection<String> getCorsAllowedOrigins() {
    return rpcHttpCorsAllowedOrigins;
  }

  @Override
  public void run() {
    // validation is performed to simulate similar behavior as with ArgGroups.
    // ArgGroups are removed because of config-file and environment based default options
    validateArgs();

    throw new ParameterException(spec.commandLine(), "Missing required subcommand");
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("logLevel", logLevel)
        .add("downstreamHttpHost", downstreamHttpHost)
        .add("downstreamHttpPort", downstreamHttpPort)
        .add("downstreamHttpPath", downstreamHttpPath)
        .add("downstreamHttpRequestTimeout", downstreamHttpRequestTimeout)
        .add("httpListenHost", httpListenHost)
        .add("httpListenPort", httpListenPort)
        .add("chainId", chainId)
        .add("dataPath", dataPath)
        .add("clientTlsOptions", clientTlsOptions)
        .add("corsAllowedOrigins", rpcHttpCorsAllowedOrigins)
        .toString();
  }

  void validateArgs() {
    final StringBuilder errorMessage = new StringBuilder();

    // required option validation
    errorMessage.append(validateRequiredOptions());

    // TLS Options validation
    errorMessage.append(picoCliTlsServerOptions.validationMessage());

    // downstream TLS validation
    errorMessage.append(clientTlsOptions.validationMessage());

    if (errorMessage.length() > 0) {
      if (errorMessage.charAt(errorMessage.length() - 1) == '\n') {
        errorMessage.deleteCharAt(errorMessage.length() - 1);
      }

      throw new InvalidCommandLineOptionsException(errorMessage.toString());
    }
  }

  private String validateRequiredOptions() {
    final List<String> missingOptions = new ArrayList<>();

    // required options validation
    if (chainId == null) {
      missingOptions.add("'--chain-id=<LONG>'");
    }

    if (downstreamHttpPort == null) {
      missingOptions.add("'--downstream-http-port=<PORT>'");
    }

    if (!missingOptions.isEmpty()) {
      return "Missing required option(s): " + String.join(",", missingOptions) + "\n";
    }
    return "";
  }
}

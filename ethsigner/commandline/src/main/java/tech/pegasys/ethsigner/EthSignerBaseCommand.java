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

import static tech.pegasys.ethsigner.DefaultCommandValues.HOST_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.LONG_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.PATH_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.PORT_FORMAT_HELP;
import static tech.pegasys.ethsigner.core.metrics.EthSignerMetricCategory.DEFAULT_METRIC_CATEGORIES;
import static tech.pegasys.ethsigner.util.RequiredOptionsUtil.checkIfRequiredOptionsAreInitialized;

import tech.pegasys.ethsigner.annotations.RequiredOption;
import tech.pegasys.ethsigner.config.AllowListHostsProperty;
import tech.pegasys.ethsigner.config.ConfigFileOption;
import tech.pegasys.ethsigner.config.InvalidCommandLineOptionsException;
import tech.pegasys.ethsigner.config.PicoCliTlsServerOptions;
import tech.pegasys.ethsigner.config.tls.client.PicoCliClientTlsOptions;
import tech.pegasys.ethsigner.convertor.MetricCategoryConverter;
import tech.pegasys.ethsigner.core.CorsAllowedOriginsProperty;
import tech.pegasys.ethsigner.core.config.Config;
import tech.pegasys.ethsigner.core.config.TlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.metrics.EthSignerMetricCategory;
import tech.pegasys.ethsigner.core.signing.ChainIdProvider;
import tech.pegasys.ethsigner.core.signing.ConfigurationChainId;
import tech.pegasys.ethsigner.util.PicoCliClientTlsOptionValidator;
import tech.pegasys.ethsigner.util.PicoCliTlsServerOptionsValidator;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Level;
import org.hyperledger.besu.metrics.StandardMetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
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
            + "Documentation can be found at https://docs.ethsigner.consensys.net.",
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
  @RequiredOption
  @Option(
      names = {"--chain-id"},
      description = "The Chain Id that will be the intended recipient for signed transactions",
      paramLabel = LONG_FORMAT_HELP,
      arity = "1")
  private Long chainId;

  @Option(
      names = {"--data-path"},
      description = "The path to a directory to store temporary files",
      paramLabel = PATH_FORMAT_HELP,
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
      description = "Host for JSON-RPC HTTP to listen on (default: 127.0.0.1)",
      paramLabel = HOST_FORMAT_HELP,
      arity = "1")
  private String httpListenHost = "127.0.0.1";

  @Option(
      names = {"--http-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: 8545)",
      paramLabel = PORT_FORMAT_HELP,
      arity = "1")
  private final Integer httpListenPort = 8545;

  @Mixin private PicoCliTlsServerOptions picoCliTlsServerOptions;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = "--downstream-http-host",
      description = "The endpoint to which received requests are forwarded (default: 127.0.0.1)",
      paramLabel = HOST_FORMAT_HELP,
      arity = "1")
  private String downstreamHttpHost = "127.0.0.1";

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @RequiredOption
  @Option(
      names = "--downstream-http-port",
      description = "The endpoint to which received requests are forwarded",
      paramLabel = PORT_FORMAT_HELP,
      arity = "1")
  private Integer downstreamHttpPort;

  private String downstreamHttpPath = "/";

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--downstream-http-path"},
      description = "The path to which received requests are forwarded (default: /)",
      defaultValue = "/",
      paramLabel = PATH_FORMAT_HELP,
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
      description = "Timeout in milliseconds to wait for downstream request (default: 5000)",
      paramLabel = LONG_FORMAT_HELP,
      arity = "1")
  private long downstreamHttpRequestTimeout = Duration.ofSeconds(5).toMillis();

  @Mixin private PicoCliClientTlsOptions clientTlsOptions;

  @Option(
      names = {"--metrics-enabled"},
      description = "Set to start the metrics exporter (default: ${DEFAULT-VALUE})")
  private final Boolean metricsEnabled = false;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @Option(
      names = {"--metrics-host"},
      paramLabel = HOST_FORMAT_HELP,
      description = "Host for the metrics exporter to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String metricsHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--metrics-port"},
      paramLabel = PORT_FORMAT_HELP,
      description = "Port for the metrics exporter to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Integer metricsPort = 9546;

  @Option(
      names = {"--metrics-category", "--metrics-categories"},
      paramLabel = "<category name>",
      split = ",",
      arity = "1..*",
      description =
          "Comma separated list of categories to track metrics for (default: ${DEFAULT-VALUE}),",
      converter = Web3signerMetricCategoryConverter.class)
  private final Set<MetricCategory> metricCategories = DEFAULT_METRIC_CATEGORIES;

  @Option(
      names = {"--metrics-host-allowlist"},
      paramLabel = "<hostname>[,<hostname>...]... or * or all",
      description =
          "Comma separated list of hostnames to allow for metrics access, or * to accept any host (default: ${DEFAULT-VALUE})",
      defaultValue = "localhost,127.0.0.1")
  private final AllowListHostsProperty metricsHostAllowList = new AllowListHostsProperty();

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--downstream-http-proxy-host"},
      description = "Hostname for proxy connect, no proxy if null (default: null)",
      paramLabel = HOST_FORMAT_HELP,
      arity = "1")
  private String httpProxyHost = null;

  @Option(
      names = {"--downstream-http-proxy-port"},
      paramLabel = PORT_FORMAT_HELP,
      description = "Port for proxy connect (default: 80)",
      arity = "1")
  private final Integer httpProxyPort = 80;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--downstream-http-proxy-username"},
      paramLabel = "<username>",
      description = "Username for proxy connect, no authentication if null (default: null)",
      arity = "1")
  private String httpProxyUsername = null;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--downstream-http-proxy-password"},
      paramLabel = "<password>",
      description = "Password for proxy connect, no authentication if null (default: null)",
      arity = "1")
  private String httpProxyPassword = null;

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
  public Boolean isMetricsEnabled() {
    return metricsEnabled;
  }

  @Override
  public Integer getMetricsPort() {
    return metricsPort;
  }

  @Override
  public String getMetricsHost() {
    return metricsHost;
  }

  @Override
  public Set<MetricCategory> getMetricCategories() {
    return metricCategories;
  }

  @Override
  public List<String> getMetricsHostAllowList() {
    return metricsHostAllowList;
  }

  @Override
  public String getHttpProxyHost() {
    return httpProxyHost;
  }

  @Override
  public Integer getHttpProxyPort() {
    return httpProxyPort;
  }

  @Override
  public String getHttpProxyUsername() {
    return httpProxyUsername;
  }

  @Override
  public String getHttpProxyPassword() {
    return httpProxyPassword;
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
        .add("httpProxyHost", httpProxyHost)
        .add("httpProxyPort", httpProxyPort)
        .add("httpProxyUsername", httpProxyUsername)
        .add("httpProxyPassword", httpProxyPassword)
        .toString();
  }

  void validateArgs() {
    checkIfRequiredOptionsAreInitialized(this);

    // Simulate ArgGroup validation (they are not used because of Config/Env default value provider)
    // TLS Options Validation
    final PicoCliTlsServerOptionsValidator picoCliTlsServerOptionsValidator =
        new PicoCliTlsServerOptionsValidator(picoCliTlsServerOptions);
    // downstream tls validation
    final PicoCliClientTlsOptionValidator picoCliClientTlsOptionValidator =
        new PicoCliClientTlsOptionValidator(clientTlsOptions);

    final String serverTlsOptionsValidationMessage =
        picoCliTlsServerOptionsValidator.validateCliOptions();
    final String downstreamTlsOptionsValidationMessage =
        picoCliClientTlsOptionValidator.validateCliOptions();

    final String errorMessage =
        serverTlsOptionsValidationMessage + downstreamTlsOptionsValidationMessage;
    if (errorMessage.trim().length() > 0) {
      throw new InvalidCommandLineOptionsException(errorMessage.trim());
    }
  }

  public static class Web3signerMetricCategoryConverter extends MetricCategoryConverter {

    public Web3signerMetricCategoryConverter() {
      addCategories(EthSignerMetricCategory.class);
      addCategories(StandardMetricCategory.class);
    }
  }
}

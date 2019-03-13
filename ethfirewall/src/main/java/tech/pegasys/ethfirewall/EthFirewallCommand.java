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
package tech.pegasys.ethfirewall;

import com.google.common.base.Suppliers;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClientOptions;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.CipherException;
import picocli.CommandLine;
import picocli.CommandLine.AbstractParseResultHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import tech.pegasys.ethfirewall.jsonrpcproxy.JsonRpcHttpService;
import tech.pegasys.ethfirewall.signing.ConfigurationChainId;
import tech.pegasys.ethfirewall.signing.TransactionSigner;

@SuppressWarnings("FieldCanBeLocal") // because Picocli injected fields report false positives
@Command(
    description = "This command runs the EthFirewall.",
    abbreviateSynopsis = true,
    name = "ethfirewall",
    mixinStandardHelpOptions = true,
    versionProvider = VersionInfo.class,
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    footer = "Ethfirewall is licensed under the Apache License 2.0")
public class EthFirewallCommand implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcHttpService.class);
  private CommandLine commandLine;

  private final Supplier<EthFirewallExceptionHandler> exceptionHandlerSupplier =
      Suppliers.memoize(EthFirewallExceptionHandler::new);

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = Level.INFO;

  @Option(
      names = {"-p", "--password"},
      description = "Password required to access the key file.",
      required = true,
      arity = "1")
  private String password;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"-k", "--keyfile"},
      description = "The path to a file containing the key used to sign transactions.",
      required = true,
      arity = "1")
  private File keyFilename;

  @Option(
      names = "--downstream-http-host",
      description = "The endpoint to which received requests are forwarded",
      arity = "1")
  private String downstreamHttpHost = "127.0.0.1";

  @Option(
      names = "--downstream-http-port",
      description = "The endpoint to which received requests are forwarded",
      arity = "1")
  private Integer downstreamHttpPort;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--http-listen-host"},
      description = "Host for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String httpListenHost = "127.0.0.1";

  @Option(
      names = {"--http-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Integer httpListenPort = 8545;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--chain-id"},
      description = "The id of the chain that the target for anu signed transactions.",
      required = true,
      arity = "1")
  private byte chainId;


  public void parse(
      final AbstractParseResultHandler<List<Object>> resultHandler,
      final EthFirewallExceptionHandler exceptionHandler,
      final String... args) {

    commandLine = new CommandLine(this);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);

    // Must manually show the usage/version info, as per the design of picocli
    // (https://picocli.info/#_printing_help_automatically)
    commandLine.parseWithHandlers(resultHandler, exceptionHandler, args);
  }

  @Override
  public void run() {
    // set log level per CLI flags
    System.out.println("Setting logging level to " + logLevel.name());
    Configurator.setAllLevels("", logLevel);

    try {
      final TransactionSigner transactionSigner =
          TransactionSigner.createFrom(keyFilename, password, new ConfigurationChainId(chainId));
      final WebClientOptions clientOptions =
          new WebClientOptions()
              .setDefaultPort(downstreamHttpPort)
              .setDefaultHost(downstreamHttpHost);
      final HttpServerOptions serverOptions =
          new HttpServerOptions()
              .setPort(httpListenPort)
              .setHost(httpListenHost)
              .setReuseAddress(true)
              .setReusePort(true);

      final Runner runner = new Runner(transactionSigner, clientOptions, serverOptions);
      runner.start();
    } catch (IOException ex) {
      LOG.info(
          "Unable to access supplied keyfile, or file does not conform to V3 keystore standard.");
    } catch (CipherException ex) {
      LOG.info("Unable to decode keyfile with supplied password.");
    }
  }

  public EthFirewallExceptionHandler exceptionHandler() {
    return exceptionHandlerSupplier.get();
  }

  // Inner class so we can get to loggingLevel.
  public class EthFirewallExceptionHandler
      extends CommandLine.AbstractHandler<List<Object>, EthFirewallExceptionHandler>
      implements CommandLine.IExceptionHandler2<List<Object>> {

    @Override
    public List<Object> handleParseException(final ParameterException ex, final String[] args) {
      if (logLevel != null && Level.DEBUG.isMoreSpecificThan(logLevel)) {
        ex.printStackTrace(err());
      } else {
        err().println(ex.getMessage());
      }
      if (!CommandLine.UnmatchedArgumentException.printSuggestions(ex, err())) {
        ex.getCommandLine().usage(err(), ansi());
      }
      return returnResultOrExit(null);
    }

    @Override
    public List<Object> handleExecutionException(
        final ExecutionException ex, final CommandLine.ParseResult parseResult) {
      return throwOrExit(ex);
    }

    @Override
    protected EthFirewallExceptionHandler self() {
      return this;
    }
  }
}

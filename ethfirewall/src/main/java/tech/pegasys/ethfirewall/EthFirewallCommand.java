package tech.pegasys.ethfirewall;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import picocli.CommandLine.AbstractParseResultHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.DefaultExceptionHandler;
import picocli.CommandLine.Option;

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
    footer = "Ethfirewall is licensed under the Apache License 2.0"
)
public class EthFirewallCommand implements Runnable {

  private static final Logger LOG = LogManager.getLogger();
  private CommandLine commandLine;

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = null;

  @Option(
      names = {"-p", "--password"},
      description = "Password required to access the key file.",
      required = true,
      arity = "1")
  private String password = null;

  @Option(
      names = {"-k", "--keyfile"},
      description = "The path to a file containing the key used to sign transactions.",
      required = true,
      arity = "1")
  private final File keyFilename = null;

  @Option(
      names = "--downstream-host",
      description = "The endpoint to which received requests are forwarded",
      required = true,
      arity = "1")
  private String web3EndpointHost;

  @Option(
      names = "--downstream-port",
      description = "The endpoint to which received requests are forwarded",
      required = true,
      arity = "1")
  private String web3EndpointPort;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--rpc-listen-host"},
      description = "Host for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      required = true,
      arity = "1")
  private String listenHost = "127.0.0.1";

  @Option(
      names = {"--rpc-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      required = true,
      arity = "1")
  private final Integer listenPort = 8545;

  public void parse(
      final PrintStream output,
      final String... args) {

    commandLine = new CommandLine(this);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);

    // Must manually show the usage/version info, as per the design of picocli
    // (https://picocli.info/#_printing_help_automatically)
    commandLine.parse(args);
    if (commandLine.isUsageHelpRequested()) {
      commandLine.usage(output);
      return;
    } else if (commandLine.isVersionHelpRequested()) {
      commandLine.printVersionHelp(System.out);
      return;
    }
  }

  @Override
  public void run() {
    // set log level per CLI flags
    if (logLevel != null) {
      System.out.println("Setting logging level to " + logLevel.name());
      Configurator.setAllLevels("", logLevel);
    }
  }
}

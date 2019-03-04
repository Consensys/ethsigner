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

import java.io.File;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

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

  private static final Logger LOG = LogManager.getLogger();
  private CommandLine commandLine;

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
      names = "--downstream-host",
      description = "The endpoint to which received requests are forwarded",
      arity = "1")
  private String downstreamHost = "127.0.0.1";

  @Option(
      names = "--downstream-port",
      description = "The endpoint to which received requests are forwarded",
      arity = "1")
  private Integer downstreamPort;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--rpc-listen-host"},
      description = "Host for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String listenHost = "127.0.0.1";

  @Option(
      names = {"--rpc-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Integer listenPort = 8545;

  public void parse(final PrintStream output, final String... args) {

    commandLine = new CommandLine(this);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);

    // Must manually show the usage/version info, as per the design of picocli
    // (https://picocli.info/#_printing_help_automatically)
    try {
      commandLine.parse(args);
      if (commandLine.isUsageHelpRequested()) {
        commandLine.usage(output);
        return;
      } else if (commandLine.isVersionHelpRequested()) {
        commandLine.printVersionHelp(System.out);
        return;
      }
    } catch (ParameterException ex) {
      output.println(ex.getMessage());
    }
  }

  @Override
  public void run() {
    // set log level per CLI flags
    System.out.println("Setting logging level to " + logLevel.name());
    Configurator.setAllLevels("", logLevel);
  }
}

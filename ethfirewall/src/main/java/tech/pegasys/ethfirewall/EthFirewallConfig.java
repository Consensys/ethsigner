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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
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
public class EthFirewallConfig {
  private static final Logger LOG = LoggerFactory.getLogger(EthFirewallConfig.class);
  private CommandLine commandLine;

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = Level.INFO;

  @Option(
      names = {"-p", "--password-file"},
      description = "The path to a file containing the passwordFile used to decrypt the keyfile.",
      required = true,
      arity = "1")
  private String passwordFilePath;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"-k", "--key-file"},
      description = "The path to a file containing the key used to sign transactions.",
      required = true,
      arity = "1")
  private File keyFile;

  @Option(
      names = "--downstream-http-host",
      description = "The endpoint to which received requests are forwarded",
      arity = "1")
  private String downstreamHttpHost = "127.0.0.1";

  @Option(
      names = "--downstream-http-port",
      description = "The endpoint to which received requests are forwarded",
      required = true,
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


  private final PrintStream output;

  public EthFirewallConfig(PrintStream output) {
    this.output = output;
  }

  public boolean parse(final String... args) {

    commandLine = new CommandLine(this);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);

    // Must manually show the usage/version info, as per the design of picocli
    // (https://picocli.info/#_printing_help_automatically)
    try {
      commandLine.parse(args);
    }
    catch(ParameterException ex) {
      handleParseException(ex);
      return false;
    }

    if(commandLine.isUsageHelpRequested()) {
      commandLine.usage(output);
      return false;
    }
    else if (commandLine.isVersionHelpRequested()) {
      commandLine.printVersionHelp(output);
      return false;
    }
    return true;
  }

  public void handleParseException(final ParameterException ex) {
    if (logLevel != null && Level.DEBUG.isMoreSpecificThan(logLevel)) {
      ex.printStackTrace(output);
    } else {
      output.println(ex.getMessage());
    }
    if (!CommandLine.UnmatchedArgumentException.printSuggestions(ex, output)) {
      ex.getCommandLine().usage(output, Ansi.AUTO);
    }
  }

  public Level getLogLevel() {
    return logLevel;
  }

  public String getPasswordFilePath() {
    return passwordFilePath;
  }

  public File getKeyFile() {
    return keyFile;
  }

  public String getDownstreamHttpHost() {
    return downstreamHttpHost;
  }

  public Integer getDownstreamHttpPort() {
    return downstreamHttpPort;
  }

  public String getHttpListenHost() {
    return httpListenHost;
  }

  public Integer getHttpListenPort() {
    return httpListenPort;
  }

}

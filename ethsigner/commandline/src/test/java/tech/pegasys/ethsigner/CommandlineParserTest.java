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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.CommandlineParser.MISSING_SUBCOMMAND_ERROR;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class CommandlineParserTest {

  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);

  private EthSignerBaseCommand config;
  private CommandlineParser parser;
  private NullSignerSubCommand subCommand;
  private String defaultUsageText;
  private String nullCommandHelp;

  @BeforeEach
  public void setup() {
    subCommand = new NullSignerSubCommand();
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outPrintStream);
    parser.registerSigner(subCommand);

    final CommandLine commandLine = new CommandLine(new EthSignerBaseCommand());
    commandLine.addSubcommand(subCommand.getCommandName(), subCommand);
    defaultUsageText = commandLine.getUsageMessage();
    nullCommandHelp =
        commandLine.getSubcommands().get(subCommand.getCommandName()).getUsageMessage();
  }

  private String parentCommandOptionsOnly() {
    return "--downstream-http-host=8.8.8.8 "
        + "--downstream-http-port=5000 "
        + "--downstream-http-request-timeout=10000 "
        + "--http-listen-port=5001 "
        + "--http-listen-host=localhost "
        + "--chain-id=6 "
        + "--logging=INFO ";
  }

  private String removeFieldFrom(final String input, final String fieldname) {
    return input.replaceAll("--" + fieldname + "=.*?(\\s|$)", "");
  }

  private String modifyField(final String input, final String fieldname, final String value) {
    return input.replaceFirst("--" + fieldname + "=.*\\b", "--" + fieldname + "=" + value);
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() throws UnknownHostException {
    final boolean result =
        parser.parseCommandLine(
            (parentCommandOptionsOnly() + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();

    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getDownstreamHttpHost()).isEqualTo(InetAddress.getByName("8.8.8.8"));
    assertThat(config.getDownstreamHttpPort()).isEqualTo(5000);
    assertThat(config.getDownstreamHttpRequestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.getHttpListenHost()).isEqualTo(InetAddress.getByName("localhost"));
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
  }

  @Test
  public void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelp() {
    final boolean result = parser.parseCommandLine("--help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(defaultUsageText);
  }

  @Test
  public void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelpWithoutDashes() {
    final boolean result = parser.parseCommandLine("help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  @Test
  public void reverseHelpRequestShowsSubCommandHelp() {
    final boolean result = parser.parseCommandLine("help", subCommand.getCommandName());
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(nullCommandHelp);
  }

  @Test
  public void missingSubCommandShowsErrorAndUsageText() {
    final boolean result = parser.parseCommandLine(parentCommandOptionsOnly().split(" "));
    assertThat(result).isFalse();
    assertThat(commandOutput.toString())
        .contains(MISSING_SUBCOMMAND_ERROR + "\n" + defaultUsageText);
  }

  @Test
  public void nonIntegerInputForDownstreamPortShowsError() {
    final String args = modifyField(parentCommandOptionsOnly(), "downstream-http-port", "abc");
    final boolean result = parser.parseCommandLine(args.split(" "));
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).contains("--downstream-http-port", "'abc' is not an int");
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
    assertThat(commandOutput.toString()).endsWith(defaultUsageText);
  }

  @Test
  public void missingRequiredParamShowsAppropriateError() {
    missingParameterShowsError("downstream-http-port");
  }

  @Test
  public void missingLoggingDefaultsToInfoLevel() {
    // Must recreate config before executions, to prevent stale data remaining in the object.
    missingOptionalParameterIsValidAndMeetsDefault("logging", config::getLogLevel, Level.INFO);
  }

  @Test
  public void missingDownStreamHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-host", config::getDownstreamHttpHost, InetAddress.getLoopbackAddress());
  }

  @Test
  public void missingDownStreamPortDefaultsTo8545() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-port", config::getHttpListenPort, 8545);
  }

  @Test
  public void missingDownstreamTimeoutDefaultsToFiveSeconds() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-request-timeout",
        config::getDownstreamHttpRequestTimeout,
        Duration.ofSeconds(5));
  }

  @Test
  public void missingListenHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-host", config::getHttpListenHost, InetAddress.getLoopbackAddress());
  }

  @Test
  public void illegalSubCommandDisplaysErrorMessage() {
    // NOTE: all required params must be specified
    final boolean result =
        parser.parseCommandLine("--downstream-http-port=8500", "--chain-id=1", "illegalSubCommand");
    assertThat(commandOutput.toString())
        .containsOnlyOnce("Did you mean: " + subCommand.getCommandName());
    assertThat(commandOutput.toString()).doesNotContain(defaultUsageText);
  }

  @Test
  public void misspeltCommandLineOptionDisplaysErrorMessage() {
    final boolean result =
        parser.parseCommandLine(
            "--downstream-http-port=8500",
            "--chain-id=1",
            "--nonExistentOption=9",
            subCommand.getCommandName());
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private void missingParameterShowsError(final String paramToRemove) {
    final String cmdLine = removeFieldFrom(parentCommandOptionsOnly(), paramToRemove);
    final boolean result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).contains("--" + paramToRemove, "Missing");
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {

    String cmdLine = removeFieldFrom(parentCommandOptionsOnly(), paramToRemove);
    cmdLine += subCommand.getCommandName();

    final boolean result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }

  @Test
  public void domainNamesDecodeIntoAnInetAddress() {
    final String input =
        "--downstream-http-host=google.com "
            + "--downstream-http-port=5000 "
            + "--downstream-http-request-timeout=10000 "
            + "--http-listen-port=5001 "
            + "--http-listen-host=localhost "
            + "--chain-id=6 "
            + "--logging=INFO";
    final String[] inputArgs = input.split(" ");

    parser.parseCommandLine(inputArgs);
    assertThat(config.getDownstreamHttpHost().getHostName()).isEqualTo("google.com");
  }

  @Test
  public void creatingSignerThrowsDisplaysFailureToCreateSignerText() {
    subCommand = new NullSignerSubCommand(true);
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outPrintStream);
    parser.registerSigner(subCommand);

    final boolean result =
        parser.parseCommandLine(
            (parentCommandOptionsOnly() + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandOutput.toString())
        .isEqualTo(
            CommandlineParser.SIGNER_CREATION_ERROR
                + "\n"
                + "Cause: "
                + NullSignerSubCommand.ERROR_MSG
                + "\n"
                + nullCommandHelp);
  }
}

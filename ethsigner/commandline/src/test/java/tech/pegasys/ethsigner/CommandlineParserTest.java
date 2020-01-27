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
import static tech.pegasys.ethsigner.CmdlineHelpers.modifyField;
import static tech.pegasys.ethsigner.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.ethsigner.CmdlineHelpers.validBaseCommandOptions;
import static tech.pegasys.ethsigner.CommandlineParser.MISSING_SUBCOMMAND_ERROR;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CommandlineParserTest {

  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);

  private EthSignerBaseCommand config;
  private CommandlineParser parser;
  private NullSignerSubCommand subCommand;
  private String defaultUsageText;
  private String nullCommandHelp;

  @BeforeEach
  void setup() {
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

  @Test
  void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result =
        parser.parseCommandLine(
            (validBaseCommandOptions() + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();

    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getDownstreamHttpHost()).isEqualTo("8.8.8.8");
    assertThat(config.getDownstreamHttpPort()).isEqualTo(5000);
    assertThat(config.getDownstreamHttpRequestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
    assertThat(config.getTlsOptions()).isNotEmpty();
    assertThat(config.getTlsOptions().get().getKeyStoreFile())
        .isEqualTo(new File("./keystore.pfx"));
    assertThat(config.getTlsOptions().get().getKeyStorePasswordFile())
        .isEqualTo(new File("./keystore.passwd"));
    assertThat(config.getTlsOptions().get().getClientAuthConstraints().get().getKnownClientsFile())
        .isEqualTo(Optional.of(new File("./known_clients")));
    assertThat(config.getClientCertificateOptions().get().getStoreFile())
        .isEqualTo(new File("./client_cert.pfx"));
    assertThat(config.getClientCertificateOptions().get().getStorePasswordFile())
        .isEqualTo(new File("./client_cert.passwd"));
    assertThat(config.getWeb3TrustStoreOptions().get().getStoreFile())
        .isEqualTo(new File("./web3_truststore.pfx"));
    assertThat(config.getWeb3TrustStoreOptions().get().getStorePasswordFile())
        .isEqualTo(new File("./web3_truststore.passwd"));
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelp() {
    final boolean result = parser.parseCommandLine("--help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(defaultUsageText);
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelpWithoutDashes() {
    final boolean result = parser.parseCommandLine("help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  @Test
  void reverseHelpRequestShowsSubCommandHelp() {
    final boolean result = parser.parseCommandLine("help", subCommand.getCommandName());
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(nullCommandHelp);
  }

  @Test
  void missingSubCommandShowsErrorAndUsageText() {
    final boolean result = parser.parseCommandLine(validBaseCommandOptions().split(" "));
    assertThat(result).isFalse();
    assertThat(commandOutput.toString())
        .contains(MISSING_SUBCOMMAND_ERROR + System.lineSeparator() + defaultUsageText);
  }

  @Test
  void nonIntegerInputForDownstreamPortShowsError() {
    final String args = modifyField(validBaseCommandOptions(), "downstream-http-port", "abc");
    final boolean result = parser.parseCommandLine(args.split(" "));
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).contains("--downstream-http-port", "'abc' is not an int");
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
    assertThat(commandOutput.toString()).endsWith(defaultUsageText);
  }

  @Test
  void missingRequiredParamShowsAppropriateError() {
    missingParameterShowsError("downstream-http-port");
  }

  @Test
  void missingLoggingDefaultsToInfoLevel() {
    // Must recreate config before executions, to prevent stale data remaining in the object.
    missingOptionalParameterIsValidAndMeetsDefault("logging", config::getLogLevel, Level.INFO);
  }

  @Test
  void missingDownStreamHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-host",
        config::getDownstreamHttpHost,
        InetAddress.getLoopbackAddress().getHostAddress());
  }

  @Test
  void missingDownStreamPortDefaultsTo8545() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-port", config::getHttpListenPort, 8545);
  }

  @Test
  void missingDownstreamTimeoutDefaultsToFiveSeconds() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-request-timeout",
        config::getDownstreamHttpRequestTimeout,
        Duration.ofSeconds(5));
  }

  @Test
  void missingListenHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-host",
        config::getHttpListenHost,
        InetAddress.getLoopbackAddress().getHostAddress());
  }

  @Test
  void illegalSubCommandDisplaysErrorMessage() {
    // NOTE: all required params must be specified
    parser.parseCommandLine("--downstream-http-port=8500", "--chain-id=1", "illegalSubCommand");
    assertThat(commandOutput.toString())
        .containsOnlyOnce("Did you mean: " + subCommand.getCommandName());
    assertThat(commandOutput.toString()).doesNotContain(defaultUsageText);
  }

  @Test
  void misspeltCommandLineOptionDisplaysErrorMessage() {
    final boolean result =
        parser.parseCommandLine(
            "--downstream-http-port=8500",
            "--chain-id=1",
            "--nonExistentOption=9",
            subCommand.getCommandName());
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private void missingParameterShowsError(final String input, final String... paramsToRemove) {
    String cmdLine = input;
    for (final String paramToRemove : paramsToRemove) {
      cmdLine = removeFieldFrom(cmdLine, paramToRemove);
    }

    final boolean result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isFalse();
    for (final String paramToRemove : paramsToRemove) {
      assertThat(commandOutput.toString()).contains("--" + paramToRemove, "Missing");
    }
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {

    String cmdLine = removeFieldFrom(validBaseCommandOptions(), paramToRemove);
    cmdLine += subCommand.getCommandName();

    final boolean result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }

  @Test
  void creatingSignerDisplaysFailureToCreateSignerText() {
    subCommand = new NullSignerSubCommand(true);
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outPrintStream);
    parser.registerSigner(subCommand);

    final boolean result =
        parser.parseCommandLine(
            (validBaseCommandOptions() + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandOutput.toString())
        .isEqualTo(
            CommandlineParser.SIGNER_CREATION_ERROR
                + System.lineSeparator()
                + "Cause: "
                + NullSignerSubCommand.ERROR_MSG
                + System.lineSeparator()
                + nullCommandHelp);
  }

  @Test
  void missingTlsKnownClientFileShowsErrorIfTlsClientAuthenticationIsNotDisabled() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "tls-known-clients-file");
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandOutput.toString())
        .contains(
            "Missing required argument(s): (--tls-allow-any-client | "
                + "[[--tls-known-clients-file=<tlsKnownClientsFile>] [--tls-allow-ca-clients]])");
  }

  @Test
  void settingTlsKnownClientAndDisablingClientAuthenticationShowsError() {
    String cmdLine = validBaseCommandOptions();
    cmdLine += "--tls-allow-any-client ";
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).contains("expected only one match but got");
  }

  @Test
  void tlsClientAuthenticationCanBeDisabledByRemovingKnownClientsAndSettingOption() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "tls-known-clients-file");
    cmdLine += "--tls-allow-any-client ";
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    assertThat(config.getTlsOptions().get().getClientAuthConstraints()).isEmpty();
  }

  @Test
  void parsingShouldFailIfTlsDisableClientAuthenticationHasAValue() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "tls-known-clients-file");
    cmdLine += "--tls-allow-any-client=false ";
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).contains("--tls-allow-any-client");
    assertThat(commandOutput.toString()).contains("should be specified without 'false' parameter");
  }

  @Test
  void missingTlsKeyStorePasswordShowsErrorWhenKeystorePasswordIsSet() {
    missingParameterShowsError(validBaseCommandOptions(), "tls-keystore-file");
  }

  @Test
  void missingTlsPasswordFileShowsErrorWhenKeyStoreIsSet() {
    missingParameterShowsError(validBaseCommandOptions(), "tls-keystore-password-file");
  }

  @Test
  void specifyingOnlyTheTlsClientWhiteListShowsError() {
    missingParameterShowsError(
        validBaseCommandOptions(), "tls-keystore-file", "tls-keystore-password-file");
  }

  @Test
  void ethSignerStartsValidlyIfNoTlsOptionsAreSet() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "tls-keystore-file");
    cmdLine = removeFieldFrom(cmdLine, "tls-keystore-password-file");
    cmdLine = removeFieldFrom(cmdLine, "tls-known-clients-file");
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    assertThat(config.getTlsOptions()).isEmpty();
  }

  @Test
  void missingClientCertificateFileDisplaysErrorIfPasswordIsStillIncluded() {
    missingParameterShowsError(validBaseCommandOptions(), "downstream-http-tls-keystore-file");
  }

  @Test
  void missingClientCertificatePasswordFileDisplaysErrorIfCertificateIsStillIncluded() {
    missingParameterShowsError(
        validBaseCommandOptions(), "downstream-http-tls-keystore-password-file");
  }

  @Test
  void cmdlineIsValidIfBothClientCertAndPasswordAreMissing() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "downstream-http-tls-keystore-file");
    cmdLine = removeFieldFrom(cmdLine, "downstream-http-tls-keystore-password-file");

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    assertThat(config.getClientCertificateOptions()).isEmpty();
  }

  @Test
  void missingDownstreamTruststoreFileDisplaysErrorIfPasswordIsStillIncluded() {
    missingParameterShowsError(validBaseCommandOptions(), "downstream-http-tls-truststore-file");
  }

  @Test
  void missingWeb3TruststorePasswordFileDisplaysErrorIfTruststoreIsStillIncluded() {
    missingParameterShowsError(
        validBaseCommandOptions(), "downstream-http-tls-truststore-password-file");
  }

  @Test
  void cmdlineIsValidIfBothWeb3TruststoreAndPasswordAreMissing() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "downstream-http-tls-truststore-file");
    cmdLine = removeFieldFrom(cmdLine, "downstream-http-tls-truststore-password-file");

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    assertThat(config.getWeb3TrustStoreOptions()).isEmpty();
  }
}

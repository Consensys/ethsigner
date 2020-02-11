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
import static tech.pegasys.ethsigner.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.ethsigner.CmdlineHelpers.validBaseCommandOptions;
import static tech.pegasys.ethsigner.util.CommandLineParserAssertions.parseCommandLineWithMissingParamsShowsError;

import tech.pegasys.ethsigner.core.config.KeyStoreOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsTrustOptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CommandlineParserClientTlsOptionsTest {

  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);

  private EthSignerBaseCommand config;
  private CommandlineParser parser;
  private NullSignerSubCommand subCommand;
  private String defaultUsageText;

  @BeforeEach
  void setup() {
    subCommand = new NullSignerSubCommand();
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outPrintStream);
    parser.registerSigner(subCommand);

    final CommandLine commandLine = new CommandLine(new EthSignerBaseCommand());
    commandLine.addSubcommand(subCommand.getCommandName(), subCommand);
    defaultUsageText = commandLine.getUsageMessage();
  }

  @Test
  void cmdLineIsValidIfOnlyDownstreamTlsIsEnabled() {
    final String cmdLine =
        removeFieldFrom(
            validBaseCommandOptions(),
            "downstream-http-tls-keystore-file",
            "downstream-http-tls-keystore-password-file",
            "downstream-http-tls-ca-auth-disabled",
            "downstream-http-tls-known-servers-file");

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();

    assertThat(optionalDownstreamTlsOptions.isPresent()).as("TLS Enabled").isTrue();
    assertThat(optionalDownstreamTlsOptions.get().getTrustOptions().isEmpty()).isTrue();
    assertThat(optionalDownstreamTlsOptions.get().getKeyStoreOptions().isEmpty()).isTrue();
  }

  @Test
  void cmdLineIsValidWithAllTlsOptions() {
    final String cmdLine = validBaseCommandOptions();

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();

    final ClientTlsOptions clientTlsOptions = optionalDownstreamTlsOptions.get();
    assertThat(clientTlsOptions.getTrustOptions().isPresent()).isTrue();
    final ClientTlsTrustOptions clientTlsTrustOptions = clientTlsOptions.getTrustOptions().get();
    assertThat(clientTlsTrustOptions.getKnownServerFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsTrustOptions.isCaAuthRequired()).isFalse();

    final KeyStoreOptions keyStoreOptions = clientTlsOptions.getKeyStoreOptions().get();
    assertThat(keyStoreOptions.getKeyStoreFile()).isEqualTo(Path.of("./test.ks"));
  }

  @Disabled("Should be enable once PicoCLI 4.2 is updated")
  @Test
  void cmdLineFailsIfDownstreamKeystoreIsUsedWithoutTlsEnabled() {
    final String cmdLine =
        removeFieldFrom(validBaseCommandOptions(), "downstream-http-tls-enabled");

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
  }

  @Test
  void missingClientCertificateFileDisplaysErrorIfPasswordIsStillIncluded() {
    String cmdLine = validBaseCommandOptions();
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        defaultUsageText,
        cmdLine,
        List.of("downstream-http-tls-keystore-file"));
  }

  @Test
  void missingClientCertificatePasswordFileDisplaysErrorIfCertificateIsStillIncluded() {
    String cmdLine = validBaseCommandOptions();
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        defaultUsageText,
        cmdLine,
        List.of("downstream-http-tls-keystore-password-file"));
  }

  @Test
  void cmdLineIsValidWhenTlsClientCertificateOptionsAreMissing() {
    final String cmdLine =
        removeFieldFrom(
            validBaseCommandOptions(),
            "downstream-http-tls-keystore-file",
            "downstream-http-tls-keystore-password-file");

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    final ClientTlsOptions clientTlsOptions = config.getClientTlsOptions().get();
    final ClientTlsTrustOptions clientTlsTrustOptions = clientTlsOptions.getTrustOptions().get();
    assertThat(clientTlsTrustOptions.getKnownServerFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsTrustOptions.isCaAuthRequired()).isFalse();
    assertThat(clientTlsOptions.getKeyStoreOptions().isEmpty()).isTrue();
  }

  @Test
  void cmdLineIsValidIfOnlyDownstreamKnownServerIsSpecified() {
    final String cmdLine =
        removeFieldFrom(
            validBaseCommandOptions(),
            "downstream-http-tls-keystore-file",
            "downstream-http-tls-keystore-password-file",
            "downstream-http-tls-ca-auth-disabled");

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();

    final ClientTlsOptions clientTlsOptions = config.getClientTlsOptions().get();
    assertThat(clientTlsOptions.getTrustOptions().isPresent()).isTrue();
    final ClientTlsTrustOptions clientTlsTrustOptions = clientTlsOptions.getTrustOptions().get();
    assertThat(clientTlsTrustOptions.getKnownServerFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsTrustOptions.isCaAuthRequired()).isTrue();
    assertThat(clientTlsOptions.getKeyStoreOptions().isEmpty()).isTrue();
  }

  @Test
  void downstreamKnownServerIsRequiredIfCASignedDisableWithoutKnownServersFile() {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        defaultUsageText,
        validBaseCommandOptions(),
        List.of("downstream-http-tls-known-servers-file"));
  }
}

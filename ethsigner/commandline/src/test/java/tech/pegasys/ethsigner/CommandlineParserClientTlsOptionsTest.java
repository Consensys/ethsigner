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

import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsCertificateOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsTrustOptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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

  @Test
  void downstreamTlsOptionsAreEmptyByDefault() {
    final String cmdLine = validBaseCommandOptions();

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isEmpty()).as("Downstream TLS Options").isTrue();
  }

  @Test
  void cmdLineIsValidIfDownstreamTlsIsEnabled() {
    String cmdLine = validBaseCommandOptions();
    cmdLine += "--downstream-http-tls-enabled ";

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();

    assertThat(optionalDownstreamTlsOptions.get().isTlsEnabled()).as("TLS Enabled").isTrue();
    assertThat(optionalDownstreamTlsOptions.get().getClientTlsTrustOptions().isEmpty()).isTrue();
    assertThat(optionalDownstreamTlsOptions.get().getClientTlsCertificateOptions().isEmpty())
        .isTrue();
  }

  @Test
  void cmdLineIsValidWithAllTlsOptions() {
    String cmdLine = validBaseCommandOptions();
    cmdLine += "--downstream-http-tls-enabled ";
    cmdLine +=
        "--downstream-http-tls-keystore-file=./test.ks --downstream-http-tls-keystore-password-file=./test.pass ";
    cmdLine +=
        "--downstream-http-tls-invalidate-ca-signed --downstream-http-tls-known-servers-file=./test.txt ";

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();

    final ClientTlsOptions clientTlsOptions = optionalDownstreamTlsOptions.get();
    assertThat(clientTlsOptions.isTlsEnabled()).as("TLS Enabled").isTrue();
    assertThat(clientTlsOptions.getClientTlsTrustOptions().isPresent()).isTrue();
    final ClientTlsTrustOptions clientTlsTrustOptions =
        clientTlsOptions.getClientTlsTrustOptions().get();
    assertThat(clientTlsTrustOptions.getKnownServerFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsTrustOptions.isCaSignedServerCertificateAllowed()).isFalse();

    final ClientTlsCertificateOptions clientTlsCertificateOptions =
        clientTlsOptions.getClientTlsCertificateOptions().get();
    assertThat(clientTlsCertificateOptions.getKeyStoreFile()).isEqualTo(Path.of("./test.ks"));
  }

  @Test
  void cmdLineFailsIfDownstreamKeystoreIsUsedWithoutTlsEnabled() {
    String cmdLine = validBaseCommandOptions();
    cmdLine +=
        "--downstream-http-tls-keystore-file=./test.ks --downstream-http-tls-keystore-password-file=./test.pass ";

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue(); // this will fail once we upgrade to picocli 4.2
  }

  @Test
  void missingClientCertificateFileDisplaysErrorIfPasswordIsStillIncluded() {
    String cmdLine = validBaseCommandOptions();
    cmdLine +=
        "--downstream-http-tls-keystore-file=./test.ks --downstream-http-tls-keystore-password-file=./test.pass ";

    missingParameterShowsError(cmdLine, "downstream-http-tls-keystore-file");
  }

  @Test
  void missingClientCertificatePasswordFileDisplaysErrorIfCertificateIsStillIncluded() {
    String cmdLine = validBaseCommandOptions();
    cmdLine +=
        "--downstream-http-tls-keystore-file=./test.ks --downstream-http-tls-keystore-password-file=./test.pass ";

    missingParameterShowsError(cmdLine, "downstream-http-tls-keystore-password-file");
  }

  @Test
  void cmdLineIsValidForAllDownstreamTrustOptions() {
    String cmdLine = validBaseCommandOptions();
    cmdLine +=
        "--downstream-http-tls-invalidate-ca-signed --downstream-http-tls-known-servers-file=./test.txt ";

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    final ClientTlsOptions clientTlsOptions = config.getClientTlsOptions().get();
    assertThat(clientTlsOptions.isTlsEnabled()).isFalse();
    final ClientTlsTrustOptions clientTlsTrustOptions =
        clientTlsOptions.getClientTlsTrustOptions().get();
    assertThat(clientTlsTrustOptions.getKnownServerFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsTrustOptions.isCaSignedServerCertificateAllowed()).isFalse();
    assertThat(clientTlsOptions.getClientTlsCertificateOptions().isEmpty()).isTrue();
  }

  @Test
  void cmdLineIsValidIfDownstreamKnownServerIsSpecified() {
    String cmdLine = validBaseCommandOptions();
    cmdLine += "--downstream-http-tls-known-servers-file=./test.txt ";

    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();

    final ClientTlsOptions clientTlsOptions = config.getClientTlsOptions().get();
    assertThat(clientTlsOptions.isTlsEnabled()).isFalse();
    assertThat(clientTlsOptions.getClientTlsTrustOptions().isPresent()).isTrue();
    final ClientTlsTrustOptions clientTlsTrustOptions =
        clientTlsOptions.getClientTlsTrustOptions().get();
    assertThat(clientTlsTrustOptions.getKnownServerFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsTrustOptions.isCaSignedServerCertificateAllowed()).isTrue();
    assertThat(clientTlsOptions.getClientTlsCertificateOptions().isEmpty()).isTrue();
  }

  @Test
  void downstreamKnownServerIsRequiredIfCASignedDisable() {
    String cmdLine = validBaseCommandOptions();
    cmdLine += "--downstream-http-tls-invalidate-ca-signed ";
    parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    missingParameterShowsError(cmdLine, "downstream-http-tls-known-servers-file");
  }
}

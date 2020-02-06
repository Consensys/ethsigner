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

import tech.pegasys.ethsigner.core.InitializationException;

import java.io.PrintWriter;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.ParameterException;

public class CommandlineParser {

  private static final Logger LOG = LogManager.getLogger();

  private final List<SignerSubCommand> signers = Lists.newArrayList();
  private final EthSignerBaseCommand baseCommand;
  private final PrintWriter stdOut;
  private final PrintWriter stdErr;

  public static final String MISSING_SUBCOMMAND_ERROR = "Signer subcommand must be defined.";
  public static final String SIGNER_CREATION_ERROR =
      "Failed to construct a signer from supplied arguments.";

  public CommandlineParser(
      final EthSignerBaseCommand baseCommand, final PrintWriter stdOut, final PrintWriter stdErr) {
    this.baseCommand = baseCommand;
    this.stdOut = stdOut;
    this.stdErr = stdErr;
  }

  public void registerSigner(final SignerSubCommand signerSubCommand) {
    signers.add(signerSubCommand);
  }

  public boolean parseCommandLine(final String... args) {

    final CommandLine commandLine = new CommandLine(baseCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setOut(stdOut);
    commandLine.setErr(stdErr);
    commandLine.setExecutionExceptionHandler(this::handleExecutionException);
    commandLine.setParameterExceptionHandler(this::handleParseException);

    for (final SignerSubCommand subcommand : signers) {
      commandLine.addSubcommand(subcommand.getCommandName(), subcommand);
    }

    final int resultCode = commandLine.execute(args);
    baseCommand.validateOptions(
        commandLine, LOG); // marked to be removed when picocli 4.2 is released
    return resultCode == CommandLine.ExitCode.OK;
  }

  private int handleParseException(ParameterException ex, String[] args) {
    if (baseCommand.getLogLevel() != null
        && Level.DEBUG.isMoreSpecificThan(baseCommand.getLogLevel())) {
      ex.printStackTrace(stdErr);
    }

    stdErr.println(ex.getMessage());

    if (!CommandLine.UnmatchedArgumentException.printSuggestions(ex, stdOut)) {
      ex.getCommandLine().usage(stdOut, Ansi.AUTO);
    }

    return ex.getCommandLine().getCommandSpec().exitCodeOnInvalidInput();
  }

  private int handleExecutionException(
      final Exception ex, final CommandLine commandLine, final CommandLine.ParseResult parseResult)
      throws Exception {
    if (!parseResult.hasSubcommand()) {
      stdErr.println(MISSING_SUBCOMMAND_ERROR);
    } else {
      if (ex instanceof TransactionSignerInitializationException) {
        stdErr.println(SIGNER_CREATION_ERROR);
        stdErr.println("Cause: " + ex.getMessage());
      } else if (ex instanceof InitializationException) {
        stdErr.println("Failed to initialize EthSigner");
        stdErr.println("Cause: " + ex.getMessage());
      } else {
        LOG.error("Ethsigner has suffered an unrecoverable failure", ex);
        stdErr.println("Ethsigner has suffered an unrecoverable failure " + ex.toString());
      }
    }
    commandLine.usage(stdOut);
    return commandLine.getCommandSpec().exitCodeOnExecutionException();
  }
}

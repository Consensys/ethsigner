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

import java.io.PrintStream;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.Level;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.RunLast;

public class CommandlineParser {

  private final List<SignerSubCommand> signers = Lists.newArrayList();
  private final EthSignerBaseCommand baseCommand;
  private final PrintStream output;

  public static final String MISSING_SUBCOMMAND_ERROR = "Signer subcommand must be defined.";

  public CommandlineParser(final EthSignerBaseCommand baseCommand, final PrintStream output) {
    this.baseCommand = baseCommand;
    this.output = output;
  }

  public void registerSigner(final SignerSubCommand signerSubCommand) {
    signers.add(signerSubCommand);
  }

  public boolean parseCommandLine(final String... args) {

    final CommandLine commandLine = new CommandLine(baseCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.setUnmatchedArgumentsAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);

    for (final SignerSubCommand subcommand : signers) {
      commandLine.addSubcommand(subcommand.getCommandName(), subcommand);
    }

    // Must manually show the usage/version info, as per the design of picocli
    // (https://picocli.info/#_printing_help_automatically)
    try {
      commandLine.parseWithHandlers(
          new RunLast().useOut(output), new ExceptionHandler<List<Object>>(), args);
      return true;
    } catch (final ParameterException ex) {
      handleParseException(ex);
      return false;
    }
  }

  public void handleParseException(final ParameterException ex) {
    if (baseCommand.getLogLevel() != null
        && Level.DEBUG.isMoreSpecificThan(baseCommand.getLogLevel())) {
      ex.printStackTrace(output);
    }

    output.println(ex.getMessage());

    if (!CommandLine.UnmatchedArgumentException.printSuggestions(ex, output)) {
      ex.getCommandLine().usage(output, Ansi.AUTO);
    }
  }

  class ExceptionHandler<R> implements CommandLine.IExceptionHandler2<R> {

    @Override
    public R handleParseException(final ParameterException ex, final String[] args) {
      throw ex;
    }

    @Override
    public R handleExecutionException(
        final CommandLine.ExecutionException ex, final CommandLine.ParseResult parseResult) {
      if (!parseResult.hasSubcommand()) {
        output.println(MISSING_SUBCOMMAND_ERROR);
        ex.getCommandLine().usage(output, Ansi.AUTO);
        return null;
      }
      throw ex;
    }
  }
}

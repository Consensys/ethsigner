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

import tech.pegasys.ethsigner.signers.filebased.FileBasedTransactionSignerCommand;
import tech.pegasys.ethsigner.signing.hashicorp.HashicorpTransactionSignerCommand;

import java.io.PrintStream;
import java.util.List;

import org.apache.logging.log4j.Level;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.RunLast;

public class EthSignerApp {

  public static void main(final String... args) {

    final PrintStream output = System.out;

    final CommandLineConfig config = new CommandLineConfig(output);
    CommandLine commandLine = new CommandLine(config);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);

    commandLine.addSubcommand(
        HashicorpTransactionSignerCommand.COMMAND_NAME, new HashicorpTransactionSignerCommand());
    commandLine.addSubcommand(
        FileBasedTransactionSignerCommand.COMMAND_NAME, new FileBasedTransactionSignerCommand());

    // Must manually show the usage/version info, as per the design of picocli
    // (https://picocli.info/#_printing_help_automatically)
    try {
      commandLine.parseWithHandlers(new RunLast(), new ExceptionHandler<List<Object>>(), args);
    } catch (final ParameterException ex) {
      handleParseException(ex);
    }

    if (commandLine.isUsageHelpRequested()) {
      commandLine.usage(output);
    } else if (commandLine.isVersionHelpRequested()) {
      commandLine.printVersionHelp(output);
    }
  }

  public static void handleParseException(final ParameterException ex) {
    /*
      if (logLevel != null && Level.DEBUG.isMoreSpecificThan(logLevel)) {
        ex.printStackTrace(output);
      } else {
        output.println(ex.getMessage());
      }
      if (!CommandLine.UnmatchedArgumentException.printSuggestions(ex, output)) {
        ex.getCommandLine().usage(output, Ansi.AUTO);
      }
    }
    */
  }

  private static class ExceptionHandler<R> implements CommandLine.IExceptionHandler2<R> {

    @Override
    public R handleParseException(final ParameterException ex, final String[] args) {
      throw new RuntimeException("Exception handled in handleParseException.", ex);
    }

    @Override
    public R handleExecutionException(
        final CommandLine.ExecutionException ex, final CommandLine.ParseResult parseResult) {
      throw new RuntimeException("Exception handled in handleParseException.", ex);
    }
  }
}

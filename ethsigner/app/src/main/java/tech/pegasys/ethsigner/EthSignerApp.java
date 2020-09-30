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

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.ethsigner.subcommands.AzureSubCommand;
import tech.pegasys.ethsigner.subcommands.CaviumSubCommand;
import tech.pegasys.ethsigner.subcommands.FileBasedSubCommand;
import tech.pegasys.ethsigner.subcommands.HSMSubCommand;
import tech.pegasys.ethsigner.subcommands.HashicorpSubCommand;
import tech.pegasys.ethsigner.subcommands.MultiKeySubCommand;
import tech.pegasys.ethsigner.subcommands.RawSubCommand;

import java.io.PrintWriter;

public class EthSignerApp {

  public static void main(final String... args) {

    final EthSignerBaseCommand baseCommand = new EthSignerBaseCommand();
    final PrintWriter outputWriter = new PrintWriter(System.out, true, UTF_8);
    final PrintWriter errorWriter = new PrintWriter(System.err, true, UTF_8);
    final CommandlineParser cmdLineParser =
        new CommandlineParser(baseCommand, outputWriter, errorWriter, System.getenv());
    cmdLineParser.registerSigners(
        new HashicorpSubCommand(),
        new FileBasedSubCommand(),
        new AzureSubCommand(),
        new HSMSubCommand(),
        new CaviumSubCommand(),
        new MultiKeySubCommand(),
        new RawSubCommand());

    cmdLineParser.parseCommandLine(args);
  }
}

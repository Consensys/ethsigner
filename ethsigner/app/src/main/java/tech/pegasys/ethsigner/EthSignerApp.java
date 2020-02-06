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

import tech.pegasys.ethsigner.signer.azure.AzureSubCommand;
import tech.pegasys.ethsigner.signer.filebased.FileBasedSubCommand;
import tech.pegasys.ethsigner.signer.hashicorp.HashicorpSubCommand;
import tech.pegasys.ethsigner.signer.multikey.MultiKeySubCommand;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class EthSignerApp {

  public static void main(final String... args) {

    final EthSignerBaseCommand baseCommand = new EthSignerBaseCommand();
    final PrintWriter out =
        new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    final PrintWriter err =
        new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true);
    final CommandlineParser cmdLineParser = new CommandlineParser(baseCommand, out, err);
    cmdLineParser.registerSigner(new HashicorpSubCommand());
    cmdLineParser.registerSigner(new FileBasedSubCommand());
    cmdLineParser.registerSigner(new AzureSubCommand());
    cmdLineParser.registerSigner(new MultiKeySubCommand());

    cmdLineParser.parseCommandLine(args);
  }
}

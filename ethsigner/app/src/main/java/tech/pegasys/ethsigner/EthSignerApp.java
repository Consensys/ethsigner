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
import tech.pegasys.ethsigner.signer.multiplatform.MultiPlatformSubCommand;

public class EthSignerApp {

  public static void main(final String... args) {

    final EthSignerBaseCommand baseCommand = new EthSignerBaseCommand();
    final CommandlineParser cmdLineParser = new CommandlineParser(baseCommand, System.out);
    cmdLineParser.registerSigner(new HashicorpSubCommand());
    cmdLineParser.registerSigner(new FileBasedSubCommand());
    cmdLineParser.registerSigner(new AzureSubCommand());
    cmdLineParser.registerSigner(new MultiPlatformSubCommand());

    cmdLineParser.parseCommandLine(args);
  }
}

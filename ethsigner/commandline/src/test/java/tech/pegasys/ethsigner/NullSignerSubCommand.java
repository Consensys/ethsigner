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

import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import picocli.CommandLine.Command;

/** Hashicorp vault related sub-command */
@Command(
    name = NullSignerSubCommand.COMMAND_NAME,
    description =
        "This command ensures that transactions are signed by a key retrieved from Hashicorp Vault.",
    mixinStandardHelpOptions = true,
    helpCommand = true)
public class NullSignerSubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "NullSigner";

  @Override
  public TransactionSigner createSigner() {
    return null;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public void run() {}
}

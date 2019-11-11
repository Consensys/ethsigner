/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.ethsigner.signer.multifilebased;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;

import java.nio.file.Path;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Multi file-based authentication related sub-command */
@Command(
    name = MultiFileBasedSubCommand.COMMAND_NAME,
    description =
        "This command ensures transactions are signed by an existing key stored in an encrypted file"
            + " that matches the sender address",
    mixinStandardHelpOptions = true)
public class MultiFileBasedSubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "multifile-based-signer";

  public MultiFileBasedSubCommand() {}

  @Spec private CommandLine.Model.CommandSpec spec; // Picocli injects reference to command spec

  @Option(
      names = {"-d", "--directory"},
      description = "The path to a directory containing the keys and password files",
      required = true,
      arity = "1")
  private Path directoryPath;

  @Override
  public TransactionSignerProvider createSignerFactory()
      throws TransactionSignerInitializationException {
    final KeyPasswordLoader keyPasswordLoader = new KeyPasswordLoader(directoryPath);
    return new MultiKeyFileTransactionSignerProvider(keyPasswordLoader);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @VisibleForTesting
  Path getDirectoryPath() {
    return directoryPath;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("directoryPath", directoryPath).toString();
  }
}

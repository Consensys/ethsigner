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
package tech.pegasys.ethsigner.signer.filebased;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.nio.file.Path;

import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** File-based authentication related sub-command */
@Command(
    name = FileBasedSubCommand.COMMAND_NAME,
    description =
        "This command ensures transactions are signed by a key stored in an encrypted file.",
    mixinStandardHelpOptions = true)
public class FileBasedSubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "file-based-signer";

  public FileBasedSubCommand() {}

  @Spec private CommandLine.Model.CommandSpec spec; // Picocli injects reference to command spec

  @Option(
      names = {"-p", "--password-file"},
      description = "The path to a file containing the passwordFile used to decrypt the keyfile.",
      required = true,
      arity = "1")
  private Path passwordFilePath;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"-k", "--key-file"},
      description = "The path to a file containing the key used to sign transactions.",
      required = true,
      arity = "1")
  private Path keyFilePath;

  @Override
  public TransactionSigner createSigner() {
    return FileBasedSignerFactory.createSigner(keyFilePath, passwordFilePath);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("passwordFilePath", passwordFilePath)
        .add("keyFilePath", keyFilePath)
        .toString();
  }
}

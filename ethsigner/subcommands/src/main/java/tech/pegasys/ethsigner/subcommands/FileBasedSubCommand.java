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
package tech.pegasys.ethsigner.subcommands;

import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.signers.secp256k1.api.SingleTransactionSignerProvider;
import tech.pegasys.signers.secp256k1.api.TransactionSigner;
import tech.pegasys.signers.secp256k1.api.TransactionSignerProvider;
import tech.pegasys.signers.secp256k1.common.TransactionSignerInitializationException;
import tech.pegasys.signers.secp256k1.filebased.FileBasedSignerFactory;

import java.nio.file.Path;

import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** File-based authentication related sub-command */
@Command(
    name = FileBasedSubCommand.COMMAND_NAME,
    description = "Sign transactions with a key stored in an encrypted V3 Keystore file.",
    mixinStandardHelpOptions = true)
public class FileBasedSubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "file-based-signer";

  public FileBasedSubCommand() {}

  @SuppressWarnings("unused") // Picocli injects reference to command spec
  @Spec
  private CommandLine.Model.CommandSpec spec;

  @Option(
      names = {"-p", "--password-file"},
      description = "The path to a file containing the password used to decrypt the keyfile.",
      required = true,
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      arity = "1")
  private Path passwordFilePath;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"-k", "--key-file"},
      description = "The path to a file containing the key used to sign transactions.",
      required = true,
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      arity = "1")
  private Path keyFilePath;

  private TransactionSigner createSigner() throws TransactionSignerInitializationException {
    return FileBasedSignerFactory.createSigner(keyFilePath, passwordFilePath);
  }

  @Override
  public TransactionSignerProvider createSignerFactory()
      throws TransactionSignerInitializationException {
    return new SingleTransactionSignerProvider(createSigner());
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

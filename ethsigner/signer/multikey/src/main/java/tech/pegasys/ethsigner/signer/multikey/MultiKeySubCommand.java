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
package tech.pegasys.ethsigner.signer.multikey;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultAuthenticator;
import tech.pegasys.ethsigner.signer.azure.AzureKeyVaultTransactionSignerFactory;
import tech.pegasys.ethsigner.signer.hashicorp.HashicorpSignerFactory;

import java.nio.file.Path;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Multi platform authentication related sub-command. Metadata config TOML files containing signing
 * information from one of several providers.
 */
@Command(
    name = MultiKeySubCommand.COMMAND_NAME,
    description =
        "This command allows Ethsigner to access multiple keys, and use the appropriate key "
            + "when signing a transaction (based on its \"from\" field). Each key's 'access' "
            + "parameters are defined in a toml file contained within the specified directory. "
            + "Keys stored in a local file, Azure or Hashicorp Vault may be accessed with an "
            + "appropriately structured TOML file."
    mixinStandardHelpOptions = true)
public class MultiKeySubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "multikey-signer";

  public MultiKeySubCommand() {}

  @Spec private CommandLine.Model.CommandSpec spec; // Picocli injects reference to command spec

  @Option(
      names = {"-d", "--directory"},
      description = "The path to a directory containing signing metadata TOML files",
      required = true,
      arity = "1")
  private Path directoryPath;

  @Override
  public TransactionSignerProvider createSignerFactory()
      throws TransactionSignerInitializationException {
    final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader =
        new SigningMetadataTomlConfigLoader(directoryPath);

    final AzureKeyVaultTransactionSignerFactory azureFactory =
        new AzureKeyVaultTransactionSignerFactory(new AzureKeyVaultAuthenticator());
    final HashicorpSignerFactory hashicorpFactory = new HashicorpSignerFactory();

    return new MultiKeyTransactionSignerProvider(
        signingMetadataTomlConfigLoader, azureFactory, hashicorpFactory);
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

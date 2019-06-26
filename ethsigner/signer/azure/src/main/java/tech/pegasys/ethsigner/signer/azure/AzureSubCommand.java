package tech.pegasys.ethsigner.signer.azure;

import picocli.CommandLine.Option;
import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

public class AzureSubCommand extends SignerSubCommand {

  @Option(
      names = {"--keyvault-name"},
      description =
          "Path to a secret in the Hashicorp vault containing the private key used for signing transactions. The "
              + "key needs to be a base 64 encoded private key for ECDSA for curve secp256k1",
      required = true,
      arity = "1")
  private String keyvaultName;


  @Option(
      names = {"--key-name"},
      required = true
  )
  private String keyName;

  @Option(
      names = {"--key-version"},
      required = true
  )
  private String keyVersion;

  @Override
  public TransactionSigner createSigner() {
    return AzureKeyVaultTransactionSigner.createFrom(keyvaultName, keyName, keyVersion);
  }

  @Override
  public String getCommandName() {
    return null;
  }
}

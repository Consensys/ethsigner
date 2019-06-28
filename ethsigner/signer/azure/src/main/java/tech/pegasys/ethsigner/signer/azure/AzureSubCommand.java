package tech.pegasys.ethsigner.signer.azure;

import com.microsoft.azure.keyvault.KeyVaultClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

/**
 * Hashicorp vault related sub-command
 */
@Command(
    name = AzureSubCommand.COMMAND_NAME,
    description =
        "This command ensures that transactions are signed by a key retrieved from Azure KMS.",
    mixinStandardHelpOptions = true)
public class AzureSubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "azure-signer";

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


  @Option(
      names = {"--client-id"},
      required = true
  )
  private String clientId;


  @Option(
      names = {"--client-secret"},
      required = true
  )
  private String clientSecret;

  @Override
  public TransactionSigner createSigner() {
    final KeyVaultClient client =
        AzureKeyVaultAuthenticator.getAuthenticatedClient(clientId, clientSecret);
    final AzureKeyVaultTransactionSignerFactory factory = new AzureKeyVaultTransactionSignerFactory(
        keyvaultName, client);
    return factory.createSigner(keyName, keyVersion);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}

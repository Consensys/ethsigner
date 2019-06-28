package tech.pegasys.ethsigner.signer.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.Bytes;
import com.microsoft.azure.keyvault.KeyIdentifier;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyItem;
import com.microsoft.azure.keyvault.models.KeyVaultErrorException;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

public class AzureKeyVaultTransactionSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  final String AZURE_URL_PATTERN = "https://%s.vault.azure.net";

  private final KeyVaultClient client;
  private final String baseUrl;

  public AzureKeyVaultTransactionSignerFactory(final String keyVaultName,
      final KeyVaultClient client) {
    this.client = client;
    this.baseUrl = String.format(AZURE_URL_PATTERN, keyVaultName);
  }

  public TransactionSigner createSigner(final String keyName, final String keyVersion) {
    checkNotNull(keyName, "keyName must be specified");

    final JsonWebKey key;
    final KeyIdentifier kid;
    try {
      kid = new KeyIdentifier(baseUrl, keyName, keyVersion);
      key = client.getKey(kid.toString()).key();
    } catch(final KeyVaultErrorException ex) {
      LOG.error("Unable to access key in vault ", ex);
      throw ex;
    } catch(final IllegalArgumentException ex) {
      LOG.error("Supplied key arguments failed validation.", ex);
      throw ex;
    } catch(final RuntimeException ex) {
      LOG.error("Failed to access the Azure key vault", ex);
      throw ex;
    }

    final byte[] rawPublicKey = Bytes.concat(key.x(), key.y());
    final BigInteger publicKey = Numeric.toBigInt(rawPublicKey);
    final String address = calculateAddress(rawPublicKey);
    return new AzureKeyVaultTransactionSigner(client, kid.toString(), publicKey, address);

  }

  private TransactionSigner createSigner(final String keyName) {
    List<KeyItem> keyVersions = client.listKeyVersions(baseUrl, keyName);
    if(keyVersions.isEmpty()) {
      LOG.error("No versions of the specified key ({}) exist in the vault ({})", keyName, baseUrl);
      throw new IllegalArgumentException("No keys of the requested name exist in the vault.");
    }
    return createSigner(keyName, keyVersions.get(keyVersions.size() - 1).toString());
  }

  // The address is the last 20 bytes of tha hash of the public key
  private String calculateAddress(final byte[] rawPublicKey) {
    final byte[] hash = Hash.sha3(rawPublicKey);
    final BigInteger addressValue = new BigInteger(Arrays.copyOfRange(hash, 12, 20));

    return "0x" + addressValue.toString(16);
  }
}

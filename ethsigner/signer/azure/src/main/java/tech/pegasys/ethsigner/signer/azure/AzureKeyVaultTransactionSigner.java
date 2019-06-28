package tech.pegasys.ethsigner.signer.azure;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKeySignatureAlgorithm;
import java.math.BigInteger;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import tech.pegasys.ethsigner.core.signing.Signature;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

public class AzureKeyVaultTransactionSigner implements TransactionSigner {

  private static final Logger LOG = LogManager.getLogger();

  private KeyVaultClient client;
  private final String keyId;
  private BigInteger publicKey;
  private String address;

  public AzureKeyVaultTransactionSigner(final KeyVaultClient client, final String keyId, final BigInteger publicKey,
      final String address) {
    this.client = client;
    this.keyId = keyId;

    this.publicKey = publicKey;
    this.address = address;
  }

  @Override
  public Signature sign(final byte[] data) {
    byte[] hash = Hash.sha3(data);
    final KeyOperationResult result =
        this.client.sign(this.keyId, new JsonWebKeySignatureAlgorithm("ECDSA256"), hash);
    final byte[] signature = result.result();

    if (signature.length != 64) {
      throw new RuntimeException(
          "Invalid signature from the keyvault signing service, must be 64 bytes long");
    }

    // reference: blog by Tomislav Markovski
    // https://tomislav.tech/2018-02-05-ethereum-keyvault-signing-transactions/
    // The output of this will be a 64 byte array. The first 32 are the value for R and the rest is S.
    final BigInteger R = new BigInteger(Arrays.copyOfRange(signature, 0, 32));
    final BigInteger S = new BigInteger(Arrays.copyOfRange(signature, 32, 32));

    // Now we have to work backwards to figure out the recId needed to recover the signature.
    // reference: https://github.com/web3j/web3j/blob/master/crypto/src/main/java/org/web3j/crypto/Sign.java
    int recId = -1;
    final ECDSASignature sig = new ECDSASignature(R, S);

    LOG.trace("public key: {}", publicKey);
    for (int i = 0; i < 4; i++) {
      final BigInteger k = Sign.recoverFromSignature(i, sig, hash);
      LOG.trace("recovered key: {}", k);
      if (k != null && k.equals(publicKey)) {
        recId = i;
        break;
      }
    }
    if (recId == -1) {
      throw new RuntimeException(
          "Could not construct a recoverable key. Are your credentials valid?");
    }

    int headerByte = recId + 27;

    return new Signature(BigInteger.valueOf(headerByte), R, S);
  }

  @Override
  public String getAddress() {
    return address;
  }
}

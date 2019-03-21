package tech.pegasys.ethfirewall.signing;

import io.vertx.core.json.JsonObject;
import java.math.BigInteger;

/**
 * The sendTransaction must contain a nonce value, otherwise it is deemed invalid.
 */
public class MandatoryTransactionNonce {

  private static final int HEXADECIMAL = 16;
  private static final int HEXADECIMAL_PREFIX_LENGTH = 2;

  public BigInteger get(final JsonObject transaction) {

    if (transaction == null || transaction.getJsonArray("params") == null) {
      throw new IllegalArgumentException("Missing params from transaction");
    }

    final JsonObject params = transaction.getJsonArray("params").getJsonObject(0);
    if (params.containsKey("nonce")) {
      return hex("nonce", params);
    }

    throw new IllegalArgumentException("Missing the nonce%n" + transaction.encodePrettily());
  }

  // TODO validate hex format - prefix 0x
  private BigInteger hex(final String key, final JsonObject params) {
    return new BigInteger(params.getString(key).substring(HEXADECIMAL_PREFIX_LENGTH), HEXADECIMAL);
  }
}

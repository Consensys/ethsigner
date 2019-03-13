package tech.pegasys.ethfirewall.signing;

/**
 * Obtains the chain ID for the blockchain that transaction are being signed.
 */
public interface ChainIdProvider {

  byte id();
}

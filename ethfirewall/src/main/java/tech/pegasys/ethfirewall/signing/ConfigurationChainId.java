package tech.pegasys.ethfirewall.signing;

public class ConfigurationChainId implements ChainIdProvider {

  private final byte id;

  public ConfigurationChainId(final byte id) {
    //TODO get chain id from config
    this.id = 0;
  }

  @Override
  public byte id() {
    return id;
  }
}

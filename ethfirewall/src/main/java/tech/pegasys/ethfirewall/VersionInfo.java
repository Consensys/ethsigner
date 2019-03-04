package tech.pegasys.ethfirewall;

import picocli.CommandLine.IVersionProvider;

public class VersionInfo implements IVersionProvider {

  @Override
  public String[] getVersion() throws Exception {
    return new String[] {"Version 0.0.1"};
  }
}

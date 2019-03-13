package tech.pegasys.ethfirewall.jsonrpcproxy.support;

import tech.pegasys.ethfirewall.Runner;
import tech.pegasys.ethfirewall.RunnerBuilder;

public class StubbedRunnerBuilder extends RunnerBuilder {

  @Override
  public Runner build() {
    return new Runner(getTransactionSigner(), getClientOptions(), getServerOptions());
  }
}

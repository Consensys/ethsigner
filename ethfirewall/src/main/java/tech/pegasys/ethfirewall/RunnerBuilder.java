package tech.pegasys.ethfirewall;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pegasys.ethfirewall.jsonrpcproxy.TransactionSigner;

public class RunnerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(RunnerBuilder.class);

  private TransactionSigner transactionSigner;
  private WebClientOptions clientOptions;
  private HttpServerOptions serverOptions;

  public RunnerBuilder() {
  }

  public TransactionSigner getTransactionSigner() {
    return transactionSigner;
  }

  public WebClientOptions getClientOptions() {
    return clientOptions;
  }

  public HttpServerOptions getServerOptions() {
    return serverOptions;
  }

  public void setTransactionSigner(TransactionSigner transactionSigner) {
    this.transactionSigner = transactionSigner;
  }

  public void setClientOptions(WebClientOptions clientOptions) {
    this.clientOptions = clientOptions;
  }

  public void setServerOptions(HttpServerOptions serverOptions) {
    this.serverOptions = serverOptions;
  }

  public Runner build() {
    if(transactionSigner == null) {
      LOG.error("Unable to construct Runner, transactionSigner is unset.");
      return null;
    }
    if(clientOptions == null) {
      LOG.error("Unable to construct Runner, clientOptions is unset.");
      return null;
    }
    if(serverOptions == null) {
      LOG.error("Unable to construct Runner, serverOptions is unset.");
      return null;
    }
    return new Runner(transactionSigner, clientOptions, serverOptions);
  }
}

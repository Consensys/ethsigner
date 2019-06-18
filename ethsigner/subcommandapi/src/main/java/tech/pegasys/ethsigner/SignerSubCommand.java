package tech.pegasys.ethsigner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import tech.pegasys.ethsigner.core.EthSigner;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

public abstract class SignerSubCommand implements Runnable  {

  private static final Logger LOG = LogManager.getLogger();

  @CommandLine.ParentCommand private CommandLineConfig config;


  public abstract TransactionSigner createSigner();

  @Override
  public void run() {
    // set log level per CLI flags
    System.out.println("Setting logging level to " + config.getLogLevel().name());
    Configurator.setAllLevels("", config.getLogLevel());

    LOG.debug("Configuration = {}", this);
    LOG.info("Version = {}", ApplicationInfo.version());

    final TransactionSigner transactionSigner = createSigner();

    final EthSigner signer = new EthSigner(config, transactionSigner);
    signer.run();
  }

}

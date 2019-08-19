package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import java.util.stream.Collectors;
import org.web3j.protocol.eea.crypto.RawPrivateTransaction;
import org.web3j.utils.Base64String;
import org.web3j.utils.Restriction;
import tech.pegasys.ethsigner.core.jsonrpc.EeaSendTransactionJsonParameters;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequestId;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

public class PantheonPrivateTransaction extends PrivateTransaction {

  public static PantheonPrivateTransaction from(
      final EeaSendTransactionJsonParameters transactionJsonParameters,
      final NonceProvider nonceProvider,
      final JsonRpcRequestId id) {
    if (!transactionJsonParameters.privacyGroupId().isPresent()) {
      throw new RuntimeException("Transaction does not contain a valid privacyGroup.");
    }

    return new PantheonPrivateTransaction(transactionJsonParameters, nonceProvider, id,
        transactionJsonParameters.privacyGroupId().get());
  }

  private final PrivacyIdentifier privacyGroupId;

  private PantheonPrivateTransaction(
      EeaSendTransactionJsonParameters transactionJsonParameters,
      NonceProvider nonceProvider,
      JsonRpcRequestId id,
      PrivacyIdentifier privacyGroupId) {
    super(transactionJsonParameters, nonceProvider, id);
    this.privacyGroupId = privacyGroupId;
  }

  @Override
  protected RawPrivateTransaction createTransaction() {
    return RawPrivateTransaction.createTransaction(
        nonce,
        transactionJsonParameters.gasPrice().orElse(DEFAULT_GAS_PRICE),
        transactionJsonParameters.gas().orElse(DEFAULT_GAS),
        transactionJsonParameters.receiver().orElse(DEFAULT_TO),
        transactionJsonParameters.data().orElse(DEFAULT_DATA),
        Base64String.wrap(transactionJsonParameters.privateFrom().getRaw()),
        Base64String.wrap(privacyGroupId.getRaw()),
        Restriction.fromString(transactionJsonParameters.restriction()));
  }
}

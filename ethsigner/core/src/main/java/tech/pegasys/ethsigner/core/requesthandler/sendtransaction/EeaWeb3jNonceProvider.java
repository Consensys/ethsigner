/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import java.io.IOException;
import java.math.BigInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.eea.Eea;

public class EeaWeb3jNonceProvider implements NonceProvider {

  private static final Logger LOG = LogManager.getLogger();

  private final Eea eea;
  private final String accountAddress;
  private String privacyGroupId;

  public EeaWeb3jNonceProvider(
      final Eea eea, final String accountAddress, final String privacyGroupId) {
    this.eea = eea;
    this.accountAddress = accountAddress;
    this.privacyGroupId = privacyGroupId;
  }

  @Override
  public BigInteger getNonce() {
    return getNonceFromClient();
  }

  private BigInteger getNonceFromClient() {
    final Request<?, EthGetTransactionCount> request =
        eea.eeaGetTransactionCount(accountAddress, privacyGroupId);
    try {
      LOG.debug("Retrieving Transaction count from eea provider for {}.", accountAddress);
      final EthGetTransactionCount count = request.send();
      final BigInteger transactionCount = count.getTransactionCount();
      LOG.trace("Reported transaction count for {} is {}", accountAddress, transactionCount);
      return transactionCount;
    } catch (final IOException e) {
      LOG.info("Failed to determine nonce from downstream handler.", e);
      throw new RuntimeException("Unable to determine nonce from eea provider.", e);
    }
  }
}

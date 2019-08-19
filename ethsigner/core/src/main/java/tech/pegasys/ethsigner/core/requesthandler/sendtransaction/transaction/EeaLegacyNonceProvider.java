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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.NonceProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Base64String;

public class EeaLegacyNonceProvider implements NonceProvider {

  private static final Logger LOG = LogManager.getLogger();

  protected final Web3jService web3jService;
  private final String accountAddress;
  private final PrivacyIdentifier privateFrom;
  private final List<PrivacyIdentifier> privateFor;

  public EeaLegacyNonceProvider(
      final Web3jService web3jService,
      final String accountAddress,
      final PrivacyIdentifier privateFrom,
      final List<PrivacyIdentifier> privateFor) {
    this.web3jService = web3jService;
    this.accountAddress = accountAddress;
    this.privateFrom = privateFrom;
    this.privateFor = privateFor;
  }

  @Override
  public BigInteger getNonce() {
    return getNonceFromClient();
  }

  private BigInteger getNonceFromClient() {

    final Request<?, EthGetTransactionCount> request =
        new Request<>(
            "eea_getTransactionCount",
            Lists.newArrayList(
                accountAddress,
                Base64String.wrap(privateFrom.getRaw()),
                privateFor.stream().map(pf -> Base64String.wrap(pf.getRaw()))),
            web3jService,
            EthGetTransactionCount.class);

    try {
      LOG.trace(
          "Retrieving Transaction count from eea provider for account {}; privateFrom {}; privateFor {}. ",
          accountAddress,
          privateFrom,
          privateFor);
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

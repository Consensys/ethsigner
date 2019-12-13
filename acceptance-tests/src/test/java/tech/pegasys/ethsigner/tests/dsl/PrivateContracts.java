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
package tech.pegasys.ethsigner.tests.dsl;

import java.io.IOException;
import java.util.Optional;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class PrivateContracts extends Contracts<PrivateTransaction> {

  private final Besu besu;
  private final Eea eea;

  public PrivateContracts(final Besu besu, final Eea eea) {
    this.besu = besu;
    this.eea = eea;
  }

  @Override
  public String sendTransaction(final PrivateTransaction smartContract) throws IOException {
    return eea.sendTransaction(smartContract);
  }

  @Override
  public Optional<? extends TransactionReceipt> getTransactionReceipt(final String hash)
      throws IOException {
    return besu.getTransactionReceipt(hash);
  }
}

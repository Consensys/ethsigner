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
package tech.pegasys.ethsigner.core.signing;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class SingleTransactionSignerProvider implements TransactionSignerProvider {

  private final TransactionSigner signer;

  public SingleTransactionSignerProvider(final TransactionSigner signer) {
    if (signer == null) {
      throw new IllegalArgumentException(
          "SingleTransactionSignerFactory requires a non-null TransactionSigner");
    }
    this.signer = signer;
  }

  @Override
  public Optional<TransactionSigner> getSigner(final String address) {
    if (signer.getAddress() != null && signer.getAddress().equalsIgnoreCase(address)) {
      return Optional.of(signer);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Set<String> availableAddresses() {
    if (signer.getAddress() != null) {
      return Set.of(signer.getAddress());
    } else {
      return Collections.emptySet();
    }
  }
}

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

import org.web3j.protocol.besu.response.privacy.PrivateTransactionReceipt;

public class Besu {

  private final org.web3j.protocol.besu.Besu besu;

  public Besu(final org.web3j.protocol.besu.Besu besu) {
    this.besu = besu;
  }

  public Optional<PrivateTransactionReceipt> getTransactionReceipt(final String hash)
      throws IOException {
    return besu.privGetTransactionReceipt(hash).send().getTransactionReceipt();
  }
}

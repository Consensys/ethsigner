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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;

import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.eea.response.PrivateTransactionReceipt;

public class Eea {

  private final org.web3j.protocol.eea.Eea eea;
  private final RawJsonRpcRequestFactory requestFactory;

  public Eea(final org.web3j.protocol.eea.Eea eea, final RawJsonRpcRequestFactory requestFactory) {
    this.eea = eea;
    this.requestFactory = requestFactory;
  }

  public String sendTransaction(final PrivateTransaction transaction) throws IOException {
    final EthSendTransaction response =
        requestFactory
            .createRequest(
                "eea_sendTransaction", singletonList(transaction), EthSendTransaction.class)
            .send();

    assertThat(response.getTransactionHash()).isNotEmpty();
    assertThat(response.getError()).isNull();

    return response.getTransactionHash();
  }

  public Optional<PrivateTransactionReceipt> getTransactionReceipt(final String hash)
      throws IOException {
    return eea.eeaGetTransactionReceipt(hash).send().getTransactionReceipt();
  }
}

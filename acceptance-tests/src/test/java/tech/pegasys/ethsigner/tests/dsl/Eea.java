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

import java.io.IOException;

import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class Eea {

  private final RawJsonRpcRequestFactory requestFactory;

  public Eea(final RawJsonRpcRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  public EthSendTransaction sendTransaction(final PrivateTransaction transaction)
      throws IOException {
    return requestFactory
        .createRequest("eea_sendTransaction", singletonList(transaction), EthSendTransaction.class)
        .send();
  }
}

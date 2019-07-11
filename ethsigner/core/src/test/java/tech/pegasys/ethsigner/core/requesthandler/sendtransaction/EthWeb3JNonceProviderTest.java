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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.EthWeb3jNonceProvider;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

@ExtendWith(MockitoExtension.class)
public class EthWeb3JNonceProviderTest {

  @Mock private Web3j web3j;

  @Mock private Request<?, EthGetTransactionCount> request;

  @Mock private EthGetTransactionCount ethGetTransactionCount;

  private final String accountAddress = "1122334455667788990011223344556677889900";

  private final BigInteger priorTransactionCount = BigInteger.TEN;

  @BeforeEach
  public void setup() throws IOException {
    when(ethGetTransactionCount.getTransactionCount()).thenReturn(priorTransactionCount);
    when(request.send()).thenReturn(ethGetTransactionCount);
    doReturn(request).when(web3j).ethGetTransactionCount(eq(accountAddress), any());
  }

  @Test
  public void returnsValueAsReceivedFromWeb3jProvider() throws IOException {
    final EthWeb3jNonceProvider nonceProvider = new EthWeb3jNonceProvider(web3j, accountAddress);

    assertThat(nonceProvider.getNonce()).isEqualTo(priorTransactionCount);

    verify(request).send();
    verify(ethGetTransactionCount).getTransactionCount();
  }
}

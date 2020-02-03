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
package tech.pegasys.ethsigner.tests.account;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.dsl.Accounts;
import tech.pegasys.ethsigner.tests.dsl.Eth;
import tech.pegasys.ethsigner.tests.dsl.tls.OkHttpClientHelpers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import okhttp3.OkHttpClient;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

public class AccountManagementAcceptanceTest extends AcceptanceTestBase {

  private static class AccountsCollation {
    public OkHttpClient client;
    public Eth eth;
    public Accounts accounts;

    public AccountsCollation(OkHttpClient client, Eth eth, Accounts accounts) {
      this.client = client;
      this.eth = eth;
      this.accounts = accounts;
    }
  }

  private AccountsCollation createAccountQuerier() {
    final OkHttpClient httpClient = OkHttpClientHelpers.createOkHttpClient(Optional.empty());
    final HttpService web3jHttpService = new HttpService(ethSigner().getUrl(), httpClient);
    Web3j jsonRpc = new JsonRpc2_0Web3j(web3jHttpService, 500, Async.defaultExecutorService());
    final Eth eth = new Eth(jsonRpc);
    return new AccountsCollation(httpClient, eth, new Accounts(eth));
  }

  @Test
  public void ethSignerAccountListHasSingleEntry() {
    final List<String> accounts = ethSigner().accounts().list();
    assertThat(accounts.size()).isEqualTo(1);
    assertThat(ethNode().accounts().balance(accounts.get(0))).isNotNull();
    assertThat(accounts.get(0)).isEqualTo("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73");
  }

  @Test
  public void ethNodeAccountListIsEmpty() {
    assertThat(ethNode().accounts().list()).isEmpty();
  }

  @Test
  void getAverageTimeToGetAccounts() throws IOException {
    final List<Long> values = Lists.newArrayList();
    long cumulativeTotal = 0;
    int maxIterations = 10;
    for (int i = 0; i < maxIterations; i++) {
      final AccountsCollation accountsCollation = createAccountQuerier();
      final long msAtStart = System.currentTimeMillis();
      accountsCollation.accounts.list();
      long timeDelta = System.currentTimeMillis() - msAtStart;
      accountsCollation.client.dispatcher().executorService().shutdown();
      accountsCollation.client.connectionPool().evictAll();
      // accountsCollation.client.cache().close();
      values.add(timeDelta);
      cumulativeTotal += timeDelta;
    }
    final double averageOperationDurationMs = cumulativeTotal / (double) maxIterations;
    System.out.println(String.format("Avg = %.2f", averageOperationDurationMs));
    System.out.println(StringUtils.join(values, ", "));
  }
}

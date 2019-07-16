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

import java.util.List;

import org.junit.jupiter.api.Test;

public class AccountManagementAcceptanceTest extends AcceptanceTestBase {

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
}

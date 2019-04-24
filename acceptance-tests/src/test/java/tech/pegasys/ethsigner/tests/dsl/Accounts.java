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
import java.math.BigInteger;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;

public class Accounts {

  /** Private key: 8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63 */
  private static final String GENESIS_ACCOUNT_ONE_PUBLIC_KEY =
      "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73";

  public static final String GENESIS_ACCOUNT_ONE_PASSWORD = "pass";
  public static final Account RICH_BENEFACTOR = new Account(GENESIS_ACCOUNT_ONE_PUBLIC_KEY);

  private final Web3j jsonRpc;

  public Accounts(final Web3j jsonRpc) {
    this.jsonRpc = jsonRpc;
  }

  public BigInteger balance(final Account account) throws IOException {
    return balance(account.address());
  }

  public BigInteger balance(final String account) throws IOException {
    return jsonRpc
        .ethGetBalance(
            account,
            DefaultBlockParameter.valueOf(jsonRpc.ethBlockNumber().send().getBlockNumber()))
        .send()
        .getBalance();
  }

  public BigInteger balance(final Account account, final BigInteger atBlock) throws IOException {
    return balance(account.address(), atBlock);
  }

  public BigInteger balance(final String account, final BigInteger atBlock) throws IOException {
    return jsonRpc
        .ethGetBalance(account, DefaultBlockParameter.valueOf(atBlock))
        .send()
        .getBalance();
  }
}

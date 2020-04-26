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
package tech.pegasys.ethsigner.jsonrpcproxy;

import java.io.IOException;

import org.web3j.crypto.CipherException;

public class DefaultProxyTestBase extends IntegrationTestBase {

  private String downstreamHttpPath;
  private boolean downstreamHttpReplacePath;

  public DefaultProxyTestBase(String downstreamHttpPath, boolean downstreamHttpReplacePath)
      throws IOException, CipherException {
    this.downstreamHttpPath = downstreamHttpPath;
    this.downstreamHttpReplacePath = downstreamHttpReplacePath;
    setupEthSigner();
  }

  private void setupEthSigner() throws IOException, CipherException {
    setupEthSigner(DEFAULT_CHAIN_ID, getDownstreamHttpPath(), isDownstreamHttpReplacePath());
  }

  protected String getDownstreamHttpPath() {
    return downstreamHttpPath;
  }

  protected boolean isDownstreamHttpReplacePath() {
    return downstreamHttpReplacePath;
  }
}

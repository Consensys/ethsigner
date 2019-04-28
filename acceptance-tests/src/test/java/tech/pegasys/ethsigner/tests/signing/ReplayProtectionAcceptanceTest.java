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
package tech.pegasys.ethsigner.tests.signing;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;

import org.junit.Ignore;
import org.junit.Test;

// TODO remove when port allocation service is deployed on Jenins CI
@Ignore
// TODO don't use the default ATBase - need to setup Patheon & EthSigner with different chain Ids
public class ReplayProtectionAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void wrongChainId() {
    // TODO value transfer - expecting error
  }

  @Test
  public void missingChainId() {
    // TODO value transfer - expecting error
  }

  @Test
  public void unecessaryChainId() {
    // TODO value transfer - expecting error
  }
}

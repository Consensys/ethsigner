/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.ethsigner.tests.dsl.signer;

import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.createJksTrustStore;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tech.pegasys.ethsigner.EthSignerApp;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodePorts;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;

public class EthSignerThreadRunner extends EthSignerBaseRunner {

  private ExecutorService executor = Executors.newSingleThreadExecutor();

  public EthSignerThreadRunner(
      SignerConfiguration signerConfig,
      NodeConfiguration nodeConfig,
      NodePorts nodePorts) {
    super(signerConfig, nodeConfig, nodePorts);
  }

  @Override
  public void launchEthSigner(final List<String> params, final String processName) {

    if (signerConfig.getOverriddenCaTrustStore().isPresent()) {
      final TlsCertificateDefinition tlsCertificateDefinition = signerConfig.getOverriddenCaTrustStore().get();
      final Path overriddenCaTrustStorePath =
          createJksTrustStore(dataPath, tlsCertificateDefinition);
      System.setProperty(
          "javax.net.ssl.trustStore", overriddenCaTrustStorePath.toAbsolutePath().toString());
      System.setProperty(
          "javax.net.ssl.trustStorePassword", tlsCertificateDefinition.getPassword());
    }

    final String[] paramsAsArray = params.toArray(new String[0]);
    executor.submit(() -> EthSignerApp.main(paramsAsArray));
  }

  @Override
  public boolean isRunning() {
    return !executor.isTerminated();
  }
}

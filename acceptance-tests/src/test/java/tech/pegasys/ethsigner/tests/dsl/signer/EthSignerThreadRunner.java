/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.tests.dsl.signer;

import static tech.pegasys.ethsigner.tests.tls.support.CertificateHelpers.createJksTrustStore;

import tech.pegasys.ethsigner.EthSignerApp;
import tech.pegasys.ethsigner.tests.dsl.node.besu.BesuNodePorts;
import tech.pegasys.ethsigner.tests.dsl.tls.TlsCertificateDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EthSignerThreadRunner extends EthSignerRunner {

  private static final Logger LOG = LogManager.getLogger();

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final CompletableFuture<Boolean> isExited = new CompletableFuture<>();

  public EthSignerThreadRunner(
      final SignerConfiguration signerConfig,
      final String hostName,
      final BesuNodePorts besuNodePorts) {
    super(signerConfig, hostName, besuNodePorts);
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  @Override
  public void launchEthSigner(final List<String> params, final String processName) {
    if (signerConfig.getOverriddenCaTrustStore().isPresent()) {
      final TlsCertificateDefinition tlsCertificateDefinition =
          signerConfig.getOverriddenCaTrustStore().get();
      final Path overriddenCaTrustStorePath =
          createJksTrustStore(dataPath, tlsCertificateDefinition);
      System.setProperty(
          "javax.net.ssl.trustStore", overriddenCaTrustStorePath.toAbsolutePath().toString());
      System.setProperty(
          "javax.net.ssl.trustStorePassword", tlsCertificateDefinition.getPassword());
    }

    final String[] paramsAsArray = params.toArray(new String[0]);
    executor.submit(
        () -> {
          try {
            EthSignerApp.main(paramsAsArray);
          } finally {
            LOG.info("Main thread has exited");
            isExited.complete(true);
          }
        });
  }

  @Override
  public boolean isRunning() {
    return !isExited.isDone();
  }

  @Override
  public void shutdown() {
    executor.shutdown();
  }
}

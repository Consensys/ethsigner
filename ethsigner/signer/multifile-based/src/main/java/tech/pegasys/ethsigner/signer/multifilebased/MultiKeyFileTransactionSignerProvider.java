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
package tech.pegasys.ethsigner.signer.multifilebased;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;
import tech.pegasys.ethsigner.signer.filebased.FileBasedSignerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiKeyFileTransactionSignerProvider implements TransactionSignerProvider {

  private static final Logger LOG = LogManager.getLogger();

  private final KeyPasswordLoader keyPasswordLoader;

  MultiKeyFileTransactionSignerProvider(final KeyPasswordLoader keyPasswordLoader) {
    this.keyPasswordLoader = keyPasswordLoader;
  }

  @Override
  public Optional<TransactionSigner> getSigner(final String address) {
    return keyPasswordLoader.loadKeyAndPasswordForAddress(address).map(this::createSigner);
  }

  @Override
  public Set<String> availableAddresses() {
    try {
      return keyPasswordLoader.loadAvailableKeys().stream()
          .map(this::createSigner)
          .filter(Objects::nonNull)
          .map(TransactionSigner::getAddress)
          .collect(Collectors.toSet());
    } catch (IOException e) {
      LOG.error("Error loading available signers", e);
      return Collections.emptySet();
    }
  }

  private TransactionSigner createSigner(final KeyPasswordFile keyPasswordFile) {
    try {
      final TransactionSigner signer =
          FileBasedSignerFactory.createSigner(
              keyPasswordFile.getKey(), keyPasswordFile.getPassword());
      LOG.debug("Loaded signer for address {}", keyPasswordFile.getAddress());
      return signer;
    } catch (TransactionSignerInitializationException e) {
      LOG.warn("Error loading signer for address {}", keyPasswordFile.getAddress(), e);
      return null;
    }
  }
}

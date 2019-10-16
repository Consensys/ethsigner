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
import tech.pegasys.ethsigner.core.signing.TransactionSignerFactory;
import tech.pegasys.ethsigner.signer.filebased.FileBasedSignerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiKeyFileTransactionSignerFactory implements TransactionSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  private final KeyPasswordLoader keyPasswordLoader;
  private final Map<String, TransactionSigner> signers = new ConcurrentHashMap<>();

  MultiKeyFileTransactionSignerFactory(final KeyPasswordLoader keyPasswordLoader) {
    this.keyPasswordLoader = keyPasswordLoader;
    try {
      keyPasswordLoader.loadAvailableKeys().forEach(this::addSigner);
    } catch (IOException e) {
      throw new TransactionSignerInitializationException("Error loading keys/passwords", e);
    }
  }

  @Override
  public Optional<TransactionSigner> getSigner(final String address) {
    if (!signers.containsKey(address)) {
      keyPasswordLoader.loadKeyPassword(Path.of(address + ".key")).ifPresent(this::addSigner);
    }
    return Optional.ofNullable(signers.getOrDefault(normalizeKey(address), null));
  }

  @Override
  public Collection<String> availableAddresses() {
    return signers.keySet();
  }

  @VisibleForTesting
  void addTransactionSigner(TransactionSigner signer) {
    final String key = signer.getAddress();
    this.signers.put(normalizeKey(key), signer);
  }

  void handleFileCreated(Path createdFile) {
    if (isKeyOrPasswordFile(createdFile)) {
      keyPasswordLoader.loadKeyPassword(createdFile).ifPresent(this::addSigner);
    }
  }

  private void addSigner(final KeyPasswordFile keyPasswordFile) {
    try {
      final TransactionSigner signer =
          FileBasedSignerFactory.createSigner(
              keyPasswordFile.getKey(), keyPasswordFile.getPassword());
      addTransactionSigner(signer);

      LOG.debug("Loaded signer for address {}", keyPasswordFile.getAddress());
    } catch (TransactionSignerInitializationException e) {
      // do nothing if we fail to load a single credential
    }
  }

  void handleFileDeleted(Path deletedFile) {
    if (isKeyOrPasswordFile(deletedFile)) {
      final String address = deletedFile.getFileName().toString().substring(0, 40);
      final TransactionSigner removedSigner = signers.remove(normalizeKey(address));

      if (removedSigner != null) {
        LOG.debug("Unloaded signer for address {}", address);
      }
    }
  }

  private boolean isKeyOrPasswordFile(final Path file) {
    return file.toFile().getName().endsWith(".key")
        || file.toFile().getName().endsWith(".password");
  }

  private String normalizeKey(final String key) {
    String normalized = key;

    if (key.startsWith("0x")) {
      normalized = normalized.substring(2);
    }

    return normalized.toLowerCase();
  }
}

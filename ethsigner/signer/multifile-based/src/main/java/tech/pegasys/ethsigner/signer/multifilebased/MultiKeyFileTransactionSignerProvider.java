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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiKeyFileTransactionSignerProvider implements TransactionSignerProvider {

  private static final Logger LOG = LogManager.getLogger();
  private static final int VALID_ADDRESS_LENGTH = 40;

  private final KeyPasswordLoader keyPasswordLoader;
  private final Map<String, TransactionSigner> signers = new HashMap<>();

  MultiKeyFileTransactionSignerProvider(final KeyPasswordLoader keyPasswordLoader) {
    this.keyPasswordLoader = keyPasswordLoader;
    try {
      keyPasswordLoader.loadAvailableKeys().forEach(this::addSigner);
    } catch (IOException e) {
      throw new TransactionSignerInitializationException("Error loading keys/passwords", e);
    }
  }

  @Override
  public Optional<TransactionSigner> getSigner(final String address) {
    final String normalizedAddress = address.toLowerCase();
    if (!signers.containsKey(normalizedAddress)) {
      keyPasswordLoader
          .loadKeyPassword(Path.of(normalizedAddress + ".key"))
          .ifPresent(this::addSigner);
    }
    return Optional.ofNullable(signers.getOrDefault(normalizedAddress, null));
  }

  @Override
  public Set<String> availableAddresses() {
    return signers.keySet();
  }

  void handleFileCreated(final Path createdFile) {
    if (isKeyOrPasswordFile(createdFile)) {
      synchronized (this) {
        keyPasswordLoader.loadKeyPassword(createdFile).ifPresent(this::addSigner);
      }
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

  @VisibleForTesting
  void addTransactionSigner(final TransactionSigner signer) {
    final String key = signer.getAddress();
    this.signers.put(normalizeAddress(key), signer);
  }

  void handleFileDeleted(final Path deletedFile) {
    if (isKeyOrPasswordFile(deletedFile)) {
      synchronized (this) {
        final String address = getAddressFromFilename(deletedFile);
        final TransactionSigner removedSigner = signers.remove(normalizeAddress(address));

        if (removedSigner != null) {
          LOG.debug("Unloaded signer for address {}", address);
        }
      }
    }
  }

  private boolean isKeyOrPasswordFile(final Path file) {
    final String filename = file.toFile().getName();
    return (filename.endsWith(".key") || filename.endsWith(".password"));
  }

  private String getAddressFromFilename(final Path deletedFile) {
    return deletedFile.getFileName().toString().substring(0, VALID_ADDRESS_LENGTH).toLowerCase();
  }

  private String normalizeAddress(final String address) {
    if (address.startsWith("0x")) {
      return address.substring(2).toLowerCase();
    } else {
      return address.toLowerCase();
    }
  }
}

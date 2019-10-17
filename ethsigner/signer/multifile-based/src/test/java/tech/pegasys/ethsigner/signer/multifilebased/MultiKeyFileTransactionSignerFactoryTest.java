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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.ADDRESS_1;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.ADDRESS_2;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.ADDRESS_3;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.loadKeyPasswordFile;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiKeyFileTransactionSignerFactoryTest {

  private KeyPasswordLoader keyPasswordLoader;
  private MultiKeyFileTransactionSignerFactory signerFactory;

  @BeforeEach
  void beforeEach() {
    keyPasswordLoader = mock(KeyPasswordLoader.class);
    signerFactory = new MultiKeyFileTransactionSignerFactory(keyPasswordLoader);
  }

  @Test
  void loadAvailableKeysOnCreation() throws Exception {
    final Set<KeyPasswordFile> keyPasswordFiles =
        Set.of(
            loadKeyPasswordFile(ADDRESS_1),
            loadKeyPasswordFile(ADDRESS_2),
            loadKeyPasswordFile(ADDRESS_3));
    when(keyPasswordLoader.loadAvailableKeys()).thenReturn(keyPasswordFiles);

    signerFactory = new MultiKeyFileTransactionSignerFactory(keyPasswordLoader);

    assertThat(signerFactory.availableAddresses()).hasSize(keyPasswordFiles.size());
  }

  @Test
  void ioExceptionOnLoadAvailableKeysDuringStartupThrowsTxSignerInitException() throws Exception {
    when(keyPasswordLoader.loadAvailableKeys()).thenThrow(new IOException());

    assertThrows(
        TransactionSignerInitializationException.class,
        () -> new MultiKeyFileTransactionSignerFactory(keyPasswordLoader));
  }

  @Test
  void handleCreatedKeyFileWithMatchingPasswordLoadsSigner() {
    final String address = ADDRESS_1;
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile(address);
    when(keyPasswordLoader.loadKeyPassword(keyPasswordFile.getKey()))
        .thenReturn(Optional.of(keyPasswordFile));

    signerFactory.handleFileCreated(keyPasswordFile.getKey());

    assertThat(signerFactory.getSigner(address)).isNotEmpty();
  }

  @Test
  void handleCreatedPasswordFileWithMatchingKeyLoadsSigner() {
    final String address = ADDRESS_1;
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile(address);
    when(keyPasswordLoader.loadKeyPassword(keyPasswordFile.getKey()))
        .thenReturn(Optional.of(keyPasswordFile));

    signerFactory.handleFileCreated(keyPasswordFile.getKey());

    assertThat(signerFactory.getSigner(address)).isNotEmpty();
  }

  @Test
  void handleCreatedKeyFileWithInvalidPasswordDoesNothing() {
    final String address = "key_with_invalid_password";
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile(address);
    when(keyPasswordLoader.loadKeyPassword(keyPasswordFile.getKey()))
        .thenReturn(Optional.of(keyPasswordFile));

    signerFactory.handleFileCreated(keyPasswordFile.getKey());

    assertThat(signerFactory.getSigner(address)).isEmpty();
  }

  @Test
  void handleDeletedKeyFileWithLoadedSignerUnloadsSigner() {
    final String address = ADDRESS_1;
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile(address);
    signerFactory.addTransactionSigner(createTransactionSigner(address));

    signerFactory.handleFileDeleted(keyPasswordFile.getKey());

    assertThat(signerFactory.getSigner(address)).isEmpty();
  }

  @Test
  void handleDeletedPasswordFileWithLoadedSignerUnloadsSigner() {
    final String address = ADDRESS_1;
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile(address);
    signerFactory.addTransactionSigner(createTransactionSigner(address));

    signerFactory.handleFileDeleted(keyPasswordFile.getPassword());

    assertThat(signerFactory.getSigner(address)).isEmpty();
  }

  @Test
  void getSignerForAvailableKeyPasswordLazyLoadsSigner() {
    final String address = ADDRESS_1;
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile(address);
    when(keyPasswordLoader.loadKeyPassword(Path.of(address + ".key")))
        .thenReturn(Optional.of(keyPasswordFile));

    assertThat(signerFactory.getSigner(address)).isNotEmpty();
  }

  @Test
  void getAvailableAddressesReturnAllLoadedSigners() {
    Collection<String> availableKeys = Set.of("foo", "bar");

    signerFactory.addTransactionSigner(createTransactionSigner("foo"));
    signerFactory.addTransactionSigner(createTransactionSigner("bar"));

    assertThat(signerFactory.availableAddresses()).containsAll(availableKeys);
  }

  @Test
  void getSignerReturnsSignerWhenOneExistsForAddress() {
    signerFactory.addTransactionSigner(createTransactionSigner("foo"));

    Optional<TransactionSigner> signer = signerFactory.getSigner("foo");

    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getAddress()).isEqualTo("foo");
  }

  @Test
  void getSignerReturnsEmptyWhenNoMatchingSignerFound() {
    Optional<TransactionSigner> signer = signerFactory.getSigner("bar");

    assertThat(signer).isEmpty();
  }

  @Test
  void getSignerIsCaseInsensitive() {
    signerFactory.addTransactionSigner(createTransactionSigner("foo"));

    assertThat(signerFactory.getSigner("foo")).isNotEmpty();
    assertThat(signerFactory.getSigner("FOO")).isNotEmpty();
  }

  private TransactionSigner createTransactionSigner(final String address) {
    TransactionSigner transactionSigner = mock(TransactionSigner.class);
    when(transactionSigner.getAddress()).thenReturn(address);
    return transactionSigner;
  }
}

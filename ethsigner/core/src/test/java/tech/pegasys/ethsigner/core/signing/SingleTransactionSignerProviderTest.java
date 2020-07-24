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
package tech.pegasys.ethsigner.core.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.support.StubbedPublicKey;
import tech.pegasys.signers.secp256k1.api.PublicKey;
import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.signers.secp256k1.api.SingleSignerProvider;

import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SingleTransactionSignerProviderTest {

  private Signer transactionSigner;
  private SingleSignerProvider signerFactory;

  @BeforeEach
  void beforeEach() {
    transactionSigner = mock(Signer.class);
    signerFactory = new SingleSignerProvider(transactionSigner);
  }

  @Test
  void whenSignerIsNullFactoryCreationFails() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new SingleSignerProvider(null));
  }

  @Test
  void whenSignerPublicKeyIsNullFactoryAvailablePublicKeysShouldReturnEmptySet() {
    when(transactionSigner.getPublicKey()).thenReturn(null);

    final Collection<PublicKey> publicKeys = signerFactory.availablePublicKeys();
    assertThat(publicKeys).isEmpty();
  }

  @Test
  void whenSignerPublicKeyIsNullFactoryGetSignerShouldReturnEmpty() {
    when(transactionSigner.getPublicKey()).thenReturn(null);

    final Optional<Signer> signer = signerFactory.getSigner(new StubbedPublicKey("0x00"));
    assertThat(signer).isEmpty();
  }

  @Test
  void whenGetSignerWithMatchingAccountShouldReturnSigner() {
    when(transactionSigner.getPublicKey()).thenReturn(new StubbedPublicKey("0x00"));

    final Optional<Signer> signer = signerFactory.getSigner(new StubbedPublicKey("0x00"));
    assertThat(signer).isNotEmpty();
  }

  @Test
  void getSignerPublicKeyIsCaseInsensitive() {
    when(transactionSigner.getPublicKey()).thenReturn(new StubbedPublicKey("0xAA"));

    assertThat(signerFactory.getSigner(new StubbedPublicKey("0xaa"))).isNotEmpty();
    assertThat(signerFactory.getSigner(new StubbedPublicKey("0xAA"))).isNotEmpty();
  }

  @Test
  void whenGetSignerWithNullAddressShouldReturnEmpty() {
    assertThat(signerFactory.getSigner(null)).isEmpty();
  }

  @Test
  void whenGetSignerWithDifferentSignerAccountShouldReturnEmpty() {
    when(transactionSigner.getPublicKey()).thenReturn(new StubbedPublicKey("0x00"));

    final Optional<Signer> signer = signerFactory.getSigner(new StubbedPublicKey("0x01"));
    assertThat(signer).isEmpty();
  }

  @Test
  void whenGetAvailablePublicKeyShouldReturnSignerAddress() {
    when(transactionSigner.getPublicKey()).thenReturn(new StubbedPublicKey("0x00"));

    final Collection<PublicKey> addresses = signerFactory.availablePublicKeys();
    assertThat(addresses).containsExactly(new StubbedPublicKey("0x00"));
  }
}

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.NO_PREFIX_LOWERCASE_KP;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.NO_PREFIX_LOWERCASE_KP_ADDRESS;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.PREFIX_LOWERCASE_KP;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.PREFIX_LOWERCASE_KP_ADDRESS;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.PREFIX_MIXEDCASE_KP;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.PREFIX_MIXEDCASE_KP_ADDRESS;
import static tech.pegasys.ethsigner.signer.multifilebased.KeyPasswordFileFixture.loadKeyPasswordFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiKeyFileTransactionSignerProviderTest {

  private KeyPasswordLoader keyPasswordLoader;
  private MultiKeyFileTransactionSignerProvider signerFactory;

  @BeforeEach
  void beforeEach() {
    keyPasswordLoader = mock(KeyPasswordLoader.class);
    signerFactory = new MultiKeyFileTransactionSignerProvider(keyPasswordLoader);
  }

  @Test
  void getSignerForAvailableKeyPasswordReturnsSigner() {
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile(NO_PREFIX_LOWERCASE_KP);
    when(keyPasswordLoader.loadKeyAndPasswordForAddress(NO_PREFIX_LOWERCASE_KP_ADDRESS))
        .thenReturn(Optional.of(keyPasswordFile));

    assertThat(signerFactory.getSigner(NO_PREFIX_LOWERCASE_KP_ADDRESS)).isNotEmpty();
  }

  @Test
  void getSignerForUnavailableKeyPasswordReturnsEmpty() {
    when(keyPasswordLoader.loadKeyAndPasswordForAddress(any())).thenReturn(Optional.empty());

    assertThat(signerFactory.getSigner(NO_PREFIX_LOWERCASE_KP_ADDRESS)).isEmpty();
  }

  @Test
  void getSignerForPasswordNotMatchingKeyReturnsEmpty() {
    final String address = "627306090abab3a6e1400e9345bc60c78a8bef57";
    final KeyPasswordFile keyPasswordFile = loadKeyPasswordFile("key_with_invalid_password");
    when(keyPasswordLoader.loadKeyAndPasswordForAddress(address))
        .thenReturn(Optional.of(keyPasswordFile));

    assertThat(signerFactory.getSigner(address)).isEmpty();
  }

  @Test
  void getAvailableAddressesReturnAllValidAddressesFromLoader() throws IOException {
    final Set<String> expectedAddressesWithHexPrefix =
        Set.of(
            "0x" + NO_PREFIX_LOWERCASE_KP_ADDRESS,
            "0x" + PREFIX_MIXEDCASE_KP_ADDRESS,
            "0x" + PREFIX_LOWERCASE_KP_ADDRESS);

    final Set<KeyPasswordFile> keyPasswordFiles =
        Set.of(
            loadKeyPasswordFile(NO_PREFIX_LOWERCASE_KP),
            loadKeyPasswordFile(PREFIX_MIXEDCASE_KP),
            loadKeyPasswordFile(PREFIX_LOWERCASE_KP));
    when(keyPasswordLoader.loadAvailableKeys()).thenReturn(keyPasswordFiles);

    assertThat(signerFactory.availableAddresses()).containsAll(expectedAddressesWithHexPrefix);
  }

  @Test
  void getAvailableAddressesReturnOnlyMatchingKeyPasswordAddressesFromLoader() throws IOException {
    final Set<String> expectedAddressesWithHexPrefix =
        Set.of("0x" + NO_PREFIX_LOWERCASE_KP_ADDRESS, "0x" + PREFIX_LOWERCASE_KP_ADDRESS);

    final Set<KeyPasswordFile> keyPasswordFiles =
        Set.of(
            loadKeyPasswordFile(NO_PREFIX_LOWERCASE_KP),
            loadKeyPasswordFile("key_with_invalid_password"),
            loadKeyPasswordFile(PREFIX_LOWERCASE_KP));
    when(keyPasswordLoader.loadAvailableKeys()).thenReturn(keyPasswordFiles);

    assertThat(signerFactory.availableAddresses()).containsAll(expectedAddressesWithHexPrefix);
  }

  @Test
  void getAvailableAddressesReturnEmptyListWhenLoaderHasNoValidKeys() throws IOException {
    when(keyPasswordLoader.loadAvailableKeys()).thenReturn(Collections.emptyList());

    assertThat(signerFactory.availableAddresses()).isEmpty();
  }
}

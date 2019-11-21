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
package tech.pegasys.ethsigner.signer.multiplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.NO_PREFIX_LOWERCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.load;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiPlatformTransactionSignerProviderTest {

  private SigningMetadataTomlConfigLoader keyPasswordLoader;
  private MultiPlatformTransactionSignerProvider signerFactory;

  @BeforeEach
  void beforeEach() {
    keyPasswordLoader = mock(SigningMetadataTomlConfigLoader.class);
    signerFactory = new MultiPlatformTransactionSignerProvider(keyPasswordLoader);
  }

  @Test
  void getSignerForAvailableMetadataReturnsSigner() {
    final FileBasedSigningMetadataFile keyPasswordFile =
        load(NO_PREFIX_LOWERCASE_ADDRESS, KEY_FILE, PASSWORD_FILE);
    when(keyPasswordLoader.loadSigningMetadataTomlForAddress(NO_PREFIX_LOWERCASE_ADDRESS))
        .thenReturn(Optional.of(keyPasswordFile));

    assertThat(signerFactory.getSigner(NO_PREFIX_LOWERCASE_ADDRESS)).isNotEmpty();
  }
}

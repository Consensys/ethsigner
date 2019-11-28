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
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.LOWERCASE_ADDRESS;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.ethsigner.signer.multiplatform.MetadataFileFixture.load;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MultiPlatformTransactionSignerProviderTest {

  private SigningMetadataTomlConfigLoader loader = mock(SigningMetadataTomlConfigLoader.class);;
  private MultiPlatformTransactionSignerProvider signerFactory =
      new MultiPlatformTransactionSignerProvider(loader);
  private final FileBasedSigningMetadataFile metadataFile =
      load(LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

  @Test
  void getSignerForAvailableMetadataReturnsSigner() {
    when(loader.loadMetadataForAddress(LOWERCASE_ADDRESS)).thenReturn(Optional.of(metadataFile));

    assertThat(signerFactory.getSigner(LOWERCASE_ADDRESS)).isNotEmpty();
  }

  @Test
  void getAddresses() {
    Collection<FileBasedSigningMetadataFile> files = Collections.singleton(metadataFile);
    when(loader.loadAvailableSigningMetadataTomlConfigs()).thenReturn(files);
    assertThat(signerFactory.availableAddresses()).containsExactly("0x" + LOWERCASE_ADDRESS);
  }
}

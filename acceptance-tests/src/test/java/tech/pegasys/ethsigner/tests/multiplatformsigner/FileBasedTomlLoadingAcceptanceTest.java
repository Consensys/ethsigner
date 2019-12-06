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
package tech.pegasys.ethsigner.tests.multiplatformsigner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

class FileBasedTomlLoadingAcceptanceTest extends MultiPlatformAcceptanceTestBase {

  static final String FILE_ETHEREUM_ADDRESS = "0xa01f618424b0113a9cebdc6cb66ca5b48e9120c5";

  @Test
  void validFileBasedTomlFileProducesSignerWhicReportsMatchingAddress() throws URISyntaxException {
    createFileBasedTomlFileAt(
        "a01f618424b0113a9cebdc6cb66ca5b48e9120c5.toml",
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath());

    setup();

    assertThat(ethSigner.accounts().list()).containsOnly(FILE_ETHEREUM_ADDRESS);
  }

  @Test
  void incorrectlyNamedFileBasedSignerIsNotLoaded() throws URISyntaxException {
    createFileBasedTomlFileAt(
        "ffffffffffffffffffffffffffffffffffffffff.toml",
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.key")
                    .toURI())
            .getAbsolutePath(),
        new File(
                Resources.getResource(
                        "UTC--2019-12-05T05-17-11.151993000Z--a01f618424b0113a9cebdc6cb66ca5b48e9120c5.password")
                    .toURI())
            .getAbsolutePath());
    setup();

    assertThat(ethSigner.accounts().list()).isEmpty();
  }
}

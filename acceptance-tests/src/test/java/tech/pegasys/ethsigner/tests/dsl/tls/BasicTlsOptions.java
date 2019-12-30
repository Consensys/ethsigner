/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.ethsigner.tests.dsl.tls;

import java.io.File;
import java.util.Optional;
import tech.pegasys.ethsigner.core.TlsOptions;

public class BasicTlsOptions implements TlsOptions {

  private final File keyStoreFile;
  private final File keyStorePasswordFile;
  private final Optional<File> knownClientsFile;

  public BasicTlsOptions(final File keyStoreFile, final File keyStorePasswordFile,
      final Optional<File> knownClientsFile) {
    this.keyStoreFile = keyStoreFile;
    this.keyStorePasswordFile = keyStorePasswordFile;
    this.knownClientsFile = knownClientsFile;
  }

  @Override
  public File getKeyStoreFile() {
    return keyStoreFile;
  }

  @Override
  public File getKeyStorePasswordFile() {
    return keyStorePasswordFile;
  }

  @Override
  public Optional<File> getKnownClientsFile() {
    return knownClientsFile;
  }
}

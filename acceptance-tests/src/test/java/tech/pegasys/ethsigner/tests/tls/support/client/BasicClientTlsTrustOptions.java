/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.tests.tls.support.client;

import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsTrustOptions;

import java.nio.file.Path;
import java.util.Optional;

public class BasicClientTlsTrustOptions implements ClientTlsTrustOptions {
  private final Optional<Path> knownServerFile;
  private final boolean isCaSignedServerCertificateAllowed;

  public BasicClientTlsTrustOptions(
      final Path knownServerFile, final boolean isCaSignedServerCertificateAllowed) {
    this.knownServerFile = Optional.ofNullable(knownServerFile);
    this.isCaSignedServerCertificateAllowed = isCaSignedServerCertificateAllowed;
  }

  @Override
  public Optional<Path> getKnownServerFile() {
    return knownServerFile;
  }

  @Override
  public boolean isCaAuthRequired() {
    return isCaSignedServerCertificateAllowed;
  }
}

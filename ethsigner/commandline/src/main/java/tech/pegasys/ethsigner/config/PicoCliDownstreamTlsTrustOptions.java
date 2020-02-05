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
package tech.pegasys.ethsigner.config;

import picocli.CommandLine.Option;
import tech.pegasys.ethsigner.core.config.DownstreamTrustOptions;

import java.nio.file.Path;
import java.util.Optional;

public class PicoCliDownstreamTlsTrustOptions implements DownstreamTrustOptions {
  @Option(
      names = "--downstream-http-tls-disallow-ca-signed",
      description = "Flag to disallow CA signed server certificates.",
      arity="0")
  private boolean tlsDisallowCaSignedCert = false;

  @Option(
      names = "--downstream-http-tls-known-servers-file",
      description = "Path to a file containing the hostname, port and certificate fingerprints of authorized servers",
      required=true,
      arity = "1"
  )
  private Path tlsknownServersFile;

  @Override
  public Optional<Path> getKnownServerFile() {
    return Optional.ofNullable(tlsknownServersFile);
  }

  @Override
  public boolean isCaSignedServerCertificateAllowed() {
    return !tlsDisallowCaSignedCert;
  }
}

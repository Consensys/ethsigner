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

import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP;

import tech.pegasys.ethsigner.core.config.ClientAuthConstraints;
import tech.pegasys.ethsigner.core.config.TlsOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class PicoCliTlsServerOptions implements TlsOptions {

  @Option(
      names = "--tls-keystore-file",
      description =
          "Path to a PKCS#12 formatted keystore; used to enable TLS on inbound connections.",
      arity = "1",
      paramLabel = MANDATORY_FILE_FORMAT_HELP)
  private File keyStoreFile;

  @Option(
      names = "--tls-keystore-password-file",
      description = "Path to a file containing the password used to decrypt the keystore.",
      arity = "1",
      paramLabel = MANDATORY_FILE_FORMAT_HELP)
  private File keyStorePasswordFile;

  public PicoCliTlsServerOptions() {}

  @Option(
      names = "--tls-allow-any-client",
      description =
          "If defined, any client may connect, regardless of presented certificate. This cannot "
              + "be set if either a whitelist or CA clients have been enabled.",
      defaultValue = "false",
      arity = "0..1")
  private Boolean tlsAllowAnyClient = false;

  @Mixin private PicoCliClientAuthConstraints clientAuthConstraints;

  public boolean isTlsEnabled() {
    return keyStoreFile != null || keyStorePasswordFile != null;
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
  public Optional<ClientAuthConstraints> getClientAuthConstraints() {
    return tlsAllowAnyClient ? Optional.empty() : Optional.of(clientAuthConstraints);
  }

  public String validationMessage() {
    if (!isTlsEnabled()) {
      return "";
    }

    final List<String> missingOptions = new ArrayList<>();

    // required options validation
    if (keyStoreFileOptionIsMissing()) {
      missingOptions.add("'--tls-keystore-file=" + MANDATORY_FILE_FORMAT_HELP + "'");
    }

    if (keystorePasswordFileOptionIsMissing()) {
      missingOptions.add("'--tls-keystore-password-file=" + MANDATORY_FILE_FORMAT_HELP + "'");
    }

    final StringBuilder errorMessage = new StringBuilder();
    if (!missingOptions.isEmpty()) {
      errorMessage
          .append("Missing required arguments(s): ")
          .append(String.join(",", missingOptions))
          .append("\n");
    }

    // ArgGroup custom validation
    if (allowAnyClientEnabledAndAuthConstraintsAreDefined()
        || allowAnyClientDisabledAndClientAuthOptionNotDefined()) {
      errorMessage.append(
          "Expecting either --tls-allow-any-client or --tls-known-clients-file=<FILE>, --tls-allow-ca-clients\n");
    }

    return errorMessage.toString();
  }

  private boolean allowAnyClientEnabledAndAuthConstraintsAreDefined() {
    return tlsAllowAnyClient
        && (clientAuthConstraints.getKnownClientsFile().isPresent()
            || clientAuthConstraints.isCaAuthorizedClientAllowed());
  }

  private boolean allowAnyClientDisabledAndClientAuthOptionNotDefined() {
    return !tlsAllowAnyClient
        && clientAuthConstraints.getKnownClientsFile().isEmpty()
        && !clientAuthConstraints.isCaAuthorizedClientAllowed();
  }

  private boolean keystorePasswordFileOptionIsMissing() {
    return keyStoreFile != null && keyStorePasswordFile == null;
  }

  private boolean keyStoreFileOptionIsMissing() {
    return keyStoreFile == null && keyStorePasswordFile != null;
  }
}

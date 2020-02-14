/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.ethsigner.signer.hashicorp;

import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_HOST_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_LONG_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_PORT_FORMAT_HELP;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.TransactionSignerInitializationException;
import tech.pegasys.ethsigner.core.signing.SingleTransactionSignerProvider;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Hashicorp vault related sub-command */
@Command(
    name = HashicorpSubCommand.COMMAND_NAME,
    description = "Sign transactions with a key stored in a Hashicorp Vault.",
    mixinStandardHelpOptions = true)
public class HashicorpSubCommand extends SignerSubCommand {
  static final String COMMAND_NAME = "hashicorp-signer";
  private static final String DEFAULT_HASHICORP_VAULT_HOST = "localhost";
  private static final String DEFAULT_KEY_PATH = "/secret/data/ethsignerSigningKey";
  private static final String DEFAULT_PORT_STRING = "8200";
  private static final Integer DEFAULT_PORT = Integer.valueOf(DEFAULT_PORT_STRING);
  private static final Long DEFAULT_TIMEOUT = Duration.ofSeconds(10).toMillis();
  public static final boolean DEFAULT_TLS_ENABLED = true;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--host"},
      description = "Host of the Hashicorp vault server (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String serverHost = DEFAULT_HASHICORP_VAULT_HOST;

  @Option(
      names = {"--port"},
      description = "Port of the Hashicorp vault server (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      arity = "1")
  private final Integer serverPort = DEFAULT_PORT;

  @Option(
      names = {"--timeout"},
      description =
          "Timeout in milliseconds for requests to the Hashicorp vault server (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      arity = "1")
  private final Long timeout = DEFAULT_TIMEOUT;

  @Option(
      names = {"--auth-file"},
      description = "Path to a File containing authentication data for Hashicorp vault",
      required = true,
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      arity = "1")
  private final Path authFilePath = null;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--signing-key-path"},
      description =
          "Path to a secret in the Hashicorp vault containing the private key used for signing transactions. The "
              + "key needs to be a base 64 encoded private key for ECDSA for curve secp256k1 (default: ${DEFAULT-VALUE})",
      paramLabel = "<SIGNING_KEY_PATH>",
      arity = "1")
  private String signingKeyPath = DEFAULT_KEY_PATH;

  @Option(
      names = {"--tls-enabled"},
      description = "Connect to Hashicorp Vault server using TLS (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Boolean tlsEnabled = DEFAULT_TLS_ENABLED;

  @Option(
      names = "--tls-known-server-file",
      description =
          "Path to the file containing Hashicorp Vault's host, port and self-signed certificate fingerprint",
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      arity = "1")
  private final Path tlsKnownServerFile = null;

  private TransactionSigner createSigner() throws TransactionSignerInitializationException {
    final HashicorpConfig config =
        new HashicorpConfig(
            signingKeyPath,
            serverHost,
            serverPort,
            authFilePath,
            timeout,
            isTlsEnabled(),
            getTlsKnownServerFile());
    return HashicorpVaultSignerFactory.createSigner(config);
  }

  @Override
  public TransactionSignerProvider createSignerFactory()
      throws TransactionSignerInitializationException {
    return new SingleTransactionSignerProvider(createSigner());
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public Optional<Path> getTlsKnownServerFile() {
    return Optional.ofNullable(tlsKnownServerFile);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("serverHost", serverHost)
        .add("serverPort", serverPort)
        .add("authFilePath", authFilePath)
        .add("timeout", timeout)
        .add("signingKeyPath", signingKeyPath)
        .add("tlsEnabled", tlsEnabled)
        .add("tlsKnownServerFile", tlsKnownServerFile)
        .toString();
  }
}

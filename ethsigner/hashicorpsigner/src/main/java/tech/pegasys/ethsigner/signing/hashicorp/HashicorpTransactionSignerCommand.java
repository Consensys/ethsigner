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
package tech.pegasys.ethsigner.signing.hashicorp;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.core.signing.TransactionSigner;

import java.nio.file.Path;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Hashicorp vault related sub-command */
@Command(
    name = HashicorpTransactionSignerCommand.COMMAND_NAME,
    description =
        "This command ensures that transactions are signed by a key retrieved from Hashicorp Vault.",
    mixinStandardHelpOptions = true,
    helpCommand = true)
public class HashicorpTransactionSignerCommand extends SignerSubCommand {

  private static final Logger LOG = LogManager.getLogger();

  public static final String COMMAND_NAME = "hashicorp-signer";
  private static final String DEFAULT_HASHICORP_VAULT_HOST = "localhost";
  private static final String DEFAULT_KEY_PATH = "/secret/data/ethsignerSigningKey";
  private static final String DEFAULT_PORT_STRING = "8200";
  private static final Integer DEFAULT_PORT = Integer.valueOf(DEFAULT_PORT_STRING);
  private static final String DEFAULT_TIMEOUT_STRING = "5";
  private static final Integer DEFAULT_TIMEOUT = Integer.valueOf(DEFAULT_TIMEOUT_STRING);

  public HashicorpTransactionSignerCommand() {}

  @Spec private CommandLine.Model.CommandSpec spec; // Picocli injects reference to command spec

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--host"},
      description = "URL of the Hashicorp vault server.",
      defaultValue = DEFAULT_HASHICORP_VAULT_HOST,
      arity = "1")
  private String serverHost = DEFAULT_HASHICORP_VAULT_HOST;

  @Option(
      names = {"--port"},
      description = "Port of the Hashicorp vault server.",
      defaultValue = DEFAULT_PORT_STRING,
      arity = "1")
  private final Integer serverPort = DEFAULT_PORT;

  @Option(
      names = {"--timeout"},
      description = "Timeout in seconds for requests to the Hashicorp vault server.",
      defaultValue = DEFAULT_TIMEOUT_STRING,
      arity = "1")
  private final Integer timeout = DEFAULT_TIMEOUT;

  @Option(
      names = {"--auth-file"},
      description = "Path to a File containing authentication data for Hashicorp vault.",
      required = true,
      arity = "1")
  private final Path authFilePath = null;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--signing-key-path"},
      description =
          "Path to a secret in the Hashicorp vault containing the private key used for signing transactions. The "
              + "key needs to be a base 64 encoded private key for ECDSA for curve secp256k1 ",
      defaultValue = DEFAULT_KEY_PATH,
      arity = "1")
  private String signingKeyPath = DEFAULT_KEY_PATH;

  @Override
  public TransactionSigner createSigner() {
    return  new HashicorpTransactionSigner(
        signingKeyPath, serverPort, serverHost, authFilePath, timeout);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("serverHost", serverHost)
        .add("serverPort", serverPort)
        .add("authFilePath", authFilePath)
        .add("timeout", timeout)
        .add("signingKeyPath", signingKeyPath)
        .toString();
  }
}

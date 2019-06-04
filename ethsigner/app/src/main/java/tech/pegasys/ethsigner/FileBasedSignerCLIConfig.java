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
package tech.pegasys.ethsigner;

import tech.pegasys.ethsigner.core.signing.fileBased.FileBasedSignerConfig;

import java.io.PrintStream;
import java.nio.file.Path;

import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Hashicorp vault authentication related sub-command */
@Command(
    name = FileBasedSignerCLIConfig.COMMAND_NAME,
    description = "This command provides file based signer related configuration data.",
    mixinStandardHelpOptions = true,
    helpCommand = true)
public class FileBasedSignerCLIConfig implements Runnable, FileBasedSignerConfig {

  public static final String COMMAND_NAME = "file-based-signer";

  public FileBasedSignerCLIConfig(final PrintStream out) {
    this.out = out;
  }

  @Spec private CommandLine.Model.CommandSpec spec; // Picocli injects reference to command spec

  private final PrintStream out;

  @Override
  public void run() {
    spec.commandLine().usage(out);
  }

  @Option(
      names = {"-p", "--password-file"},
      description = "The path to a file containing the passwordFile used to decrypt the keyfile.",
      required = true,
      arity = "1")
  private Path passwordFilePath;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"-k", "--key-file"},
      description = "The path to a file containing the key used to sign transactions.",
      required = true,
      arity = "1")
  private Path keyFile;

  @Override
  public Path getPasswordFilePath() {
    return passwordFilePath;
  }

  @Override
  public Path getKeyPath() {
    return keyFile;
  }

  @Override
  public boolean isConfigured() {
    return passwordFilePath != null && keyFile != null;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("passwordFilePath", passwordFilePath)
        .add("keyFile", keyFile)
        .toString();
  }
}

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
package tech.pegasys.ethsigner.subcommands;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.EthSignerBaseCommand;

import java.nio.file.Paths;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class FileBasedSubCommandTest {

  private static final String PASSWORD_FILE =
      Paths.get("/this/is/the/path/to/the/password/file").toString();
  private static final String KEY_FILE = Paths.get("/this/is/the/path/to/the/key/file").toString();
  private static final EthSignerBaseCommand CONFIG = new EthSignerBaseCommand();

  private FileBasedSubCommand fileBasedSubCommand;

  private boolean parseCommand(final String cmdLine) {
    fileBasedSubCommand = new FileBasedSubCommand(CONFIG);
    final CommandLine commandLine = new CommandLine(fileBasedSubCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);

    try {
      commandLine.parse(cmdLine.split(" "));
    } catch (final CommandLine.ParameterException e) {
      return false;
    }
    return true;
  }

  private String validCommandLine() {
    return "--password-file=" + PASSWORD_FILE + " --key-file=" + KEY_FILE;
  }

  private String removeFieldFrom(final String input, final String fieldname) {
    return input.replaceAll("--" + fieldname + "=.*?(\\s|$)", "");
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result = parseCommand(validCommandLine());

    assertThat(result).isTrue();
    assertThat(fileBasedSubCommand.toString()).contains(PASSWORD_FILE);
    assertThat(fileBasedSubCommand.toString()).contains(KEY_FILE);
  }

  @Test
  public void missingRequiredParamShowsAppropriateError() {
    missingParameterShowsError("password-file");
    missingParameterShowsError("key-file");
  }

  private void missingParameterShowsError(final String paramToRemove) {
    final String cmdLine = removeFieldFrom(validCommandLine(), paramToRemove);
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }
}

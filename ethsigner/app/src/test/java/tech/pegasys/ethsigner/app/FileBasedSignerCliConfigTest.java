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
package tech.pegasys.ethsigner.app;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.FileBasedSignerCliConfig;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import picocli.CommandLine;

public class FileBasedSignerCliConfigTest {

  private static final String PASSWORD_FILE = "/this/is/the/path/to/the/password/file";
  private static final String KEY_FILE = "/this/is/the/path/to/the/key/file";
  private CommandLine commandLine;
  private FileBasedSignerCliConfig config;

  private boolean parseCommand(final String cmdLine) {
    config = new FileBasedSignerCliConfig();
    commandLine = new CommandLine(config);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);

    try {
      commandLine.parse(cmdLine.split(" "));
    } catch (CommandLine.ParameterException e) {
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
    assertThat(config.getPasswordFilePath().toString()).isEqualTo(PASSWORD_FILE);
    assertThat(config.getKeyPath().toString()).isEqualTo(KEY_FILE);
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

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

import tech.pegasys.ethsigner.FileBasedSignerCLIConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import picocli.CommandLine;

public class FileBasedSignerCLIConfigTest {

  private static final String THIS_IS_THE_PATH_TO_THE_FILE = "/this/is/the/path/to/the/file";
  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);
  private CommandLine commandLine;
  private FileBasedSignerCLIConfig config;

  private boolean parseCommand(final String cmdLine) {
    config = new FileBasedSignerCLIConfig(outPrintStream);
    commandLine = new CommandLine(config);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);

    try {
      commandLine.parse(cmdLine.split(" "));
    } catch (CommandLine.ParameterException e) {
      outPrintStream.println(e.getMessage());
      return false;
    }
    return true;
  }

  private String validCommandLine() {
    return "--password-file="
        + THIS_IS_THE_PATH_TO_THE_FILE
        + " --key-file="
        + THIS_IS_THE_PATH_TO_THE_FILE;
  }

  private String removeFieldFrom(final String input, final String fieldname) {
    return input.replaceAll("--" + fieldname + "=.*?(\\s|$)", "");
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result = parseCommand(validCommandLine());

    assertThat(result).isTrue();
    assertThat(config.getPasswordFilePath().toString()).isEqualTo(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(config.getKeyPath().toString()).isEqualTo(THIS_IS_THE_PATH_TO_THE_FILE);
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
    assertThat(commandOutput.toString()).contains("--" + paramToRemove, "Missing");
  }
}

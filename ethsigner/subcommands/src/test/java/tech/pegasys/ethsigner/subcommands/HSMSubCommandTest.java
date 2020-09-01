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

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class HSMSubCommandTest {

  private static final String library = "/this/is/the/path/to/the/library/library.so";
  private static final String slot = "slot";
  private static final String pin = "pin";
  private static final String address = "0x";

  private HSMSubCommand config;

  private boolean parseCommand(final String cmdLine) {
    config = new HSMSubCommand();
    final CommandLine commandLine = new CommandLine(config);
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
    return "--library="
        + library
        + " --slot-label="
        + slot
        + " --slot-pin="
        + pin
        + " --eth-address="
        + address;
  }

  private String removeFieldFrom(final String input, final String fieldname) {
    return input.replaceAll("--" + fieldname + "=.*?(\\s|$)", "");
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result = parseCommand(validCommandLine());

    assertThat(result).isTrue();
    assertThat(config.toString()).contains(library);
    assertThat(config.toString()).contains(slot);
    assertThat(config.toString()).contains(address);
  }

  @Test
  public void missingRequiredParamShowsAppropriateError() {
    missingParameterShowsError("library");
    missingParameterShowsError("slot-label");
    missingParameterShowsError("slot-pin");
    missingParameterShowsError("eth-address");
  }

  private void missingParameterShowsError(final String paramToRemove) {
    final String cmdLine = removeFieldFrom(validCommandLine(), paramToRemove);
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }
}

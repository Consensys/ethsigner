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

import tech.pegasys.ethsigner.HashicorpTransactionSignerCliConfig;

import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import picocli.CommandLine;

public class HashicorpTransactionSignerCliConfigTest {

  private static final String THIS_IS_THE_PATH_TO_THE_FILE = "/this/is/the/path/to/the/file";
  private static final String HTTP_HOST_COM = "http://host.com";
  private static final String PORT = "23000";
  private static final String PATH_TO_SIGNING_KEY = "/path/to/signing/key";
  private static final String FIFTEEN = "15";
  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private HashicorpTransactionSignerCliConfig hashiConfig;

  private boolean parseCommand(final String cmdLine) {
    hashiConfig = new HashicorpTransactionSignerCliConfig();
    final CommandLine commandLine = new CommandLine(hashiConfig);
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
    return "--auth-file="
        + THIS_IS_THE_PATH_TO_THE_FILE
        + " --host="
        + HTTP_HOST_COM
        + " --port="
        + PORT
        + " --signing-key-path="
        + PATH_TO_SIGNING_KEY
        + " --timeout="
        + FIFTEEN;
  }

  private String removeFieldFrom(final String input, final String fieldname) {
    return input.replaceAll("--" + fieldname + "=.*?(\\s|$)", "");
  }

  private String modifyField(final String input, final String fieldname, final String value) {
    return input.replaceFirst("--" + fieldname + "=.*\\b", "--" + fieldname + "=" + value);
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result = parseCommand(validCommandLine());

    assertThat(result).isTrue();
    final String jsonString = hashiConfig.jsonString();
    assertThat(jsonString).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(jsonString).contains(HTTP_HOST_COM);
    assertThat(jsonString).contains(PORT);
    assertThat(jsonString).contains(PATH_TO_SIGNING_KEY);
    assertThat(jsonString).contains(FIFTEEN);
  }

  @Test
  public void nonIntegerInputForPortShowsError() {
    final String cmdLine = modifyField(validCommandLine(), "port", "noInteger");
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }

  @Test
  public void nonIntegerInputForTimeoutShowsError() {
    final String cmdLine = modifyField(validCommandLine(), "timeout", "noInteger");
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }

  @Test
  public void missingRequiredParamShowsAppropriateError() {
    missingParameterShowsError("auth-file");
  }

  @Test
  public void missingOptionalParametersAreSetToDefault() {
    // Must recreate commandLineConfig before executions, to prevent stale data remaining in the
    // object.
    HashicorpTransactionSignerCliConfig hcConfig = new HashicorpTransactionSignerCliConfig();
    missingOptionalParameterIsValidAndMeetsDefault("host", hcConfig::jsonString, "localhost");

    hcConfig = new HashicorpTransactionSignerCliConfig();
    missingOptionalParameterIsValidAndMeetsDefault("host", hcConfig::jsonString, "8200");

    hcConfig = new HashicorpTransactionSignerCliConfig();
    missingOptionalParameterIsValidAndMeetsDefault(
        "host", hcConfig::jsonString, "/secret/data/ethsignerSigningKey");
  }

  private void missingParameterShowsError(final String paramToRemove) {
    final String cmdLine = removeFieldFrom(validCommandLine(), paramToRemove);
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove,
      final Supplier<String> actualValueGetter,
      final String expectedValue) {
    final String cmdLine = removeFieldFrom(validCommandLine(), paramToRemove);
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).contains(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }
}

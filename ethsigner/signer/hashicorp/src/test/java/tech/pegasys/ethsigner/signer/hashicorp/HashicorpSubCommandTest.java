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
package tech.pegasys.ethsigner.signer.hashicorp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class HashicorpSubCommandTest {

  public static final String CLIENT_CERT_PFX = "./client_cert.pfx";
  private static final String THIS_IS_THE_PATH_TO_THE_FILE =
      Paths.get("/this/is/the/path/to/the/file").toString();
  private static final String HTTP_HOST_COM = "http://host.com";
  private static final String PORT = "23000";
  private static final String PATH_TO_SIGNING_KEY = Paths.get("/path/to/signing/key").toString();
  private static final String FIFTEEN = "15";
  public static final String CLIENT_CERT_PASSWD = "./client_cert.passwd";
  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();

  private HashicorpSubCommand hashiConfig;

  private boolean parseCommand(final String cmdLine) {
    hashiConfig = new HashicorpSubCommand();
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
        + FIFTEEN
        + " --tls-server-truststore-file="
        + CLIENT_CERT_PFX
        + " --tls-server-truststore-password-file="
        + CLIENT_CERT_PASSWD;
  }

  private String removeFieldFrom(final String input, final String fieldName) {
    return input.replaceAll("--" + fieldName + "=.*?(\\s|$)", "");
  }

  private String modifyField(final String input, final String fieldName, final String value) {
    return input.replaceFirst("--" + fieldName + "=.*\\b", "--" + fieldName + "=" + value);
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result = parseCommand(validCommandLine());

    assertThat(result).isTrue();
    final String string = hashiConfig.toString();
    assertThat(string).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(string).contains(HTTP_HOST_COM);
    assertThat(string).contains(PORT);
    assertThat(string).contains(PATH_TO_SIGNING_KEY);
    assertThat(string).contains(FIFTEEN);

    assertThat(hashiConfig.getTlsOptions().get().getStoreFile())
        .isEqualTo(new File(CLIENT_CERT_PFX));
    assertThat(hashiConfig.getTlsOptions().get().getStorePasswordFile())
        .isEqualTo(new File(CLIENT_CERT_PASSWD));
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
    HashicorpSubCommand hcConfig = new HashicorpSubCommand();
    missingOptionalParameterIsValidAndMeetsDefault("host", hcConfig::toString, "localhost");

    hcConfig = new HashicorpSubCommand();
    missingOptionalParameterIsValidAndMeetsDefault("host", hcConfig::toString, "8200");

    hcConfig = new HashicorpSubCommand();
    missingOptionalParameterIsValidAndMeetsDefault(
        "host", hcConfig::toString, "/secret/data/ethsignerSigningKey");
  }

  @Test
  void missingClientCertificateFileDisplaysErrorIfPasswordIsStillIncluded() {
    missingParameterShowsError("tls-server-truststore-file");
  }

  @Test
  void missingClientCertificatePasswordFileDisplaysErrorIfCertificateIsStillIncluded() {
    missingParameterShowsError("tls-server-truststore-password-file");
  }

  @Test
  void cmdlineIsValidIfBothClientCertAndPasswordAreMissing() {
    String cmdLine = validCommandLine();
    cmdLine = removeFieldFrom(cmdLine, "tls-server-truststore-file");
    cmdLine = removeFieldFrom(cmdLine, "tls-server-truststore-password-file");

    final boolean result = parseCommand(cmdLine);

    assertThat(result).isTrue();
    assertThat(hashiConfig.getTlsOptions()).isEmpty();
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

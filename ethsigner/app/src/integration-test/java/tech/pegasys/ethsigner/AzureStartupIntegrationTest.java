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
package tech.pegasys.ethsigner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.signer.azure.AzureSubCommand;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

public class AzureStartupIntegrationTest {

  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);
  private EthSignerBaseCommand baseCommand = new EthSignerBaseCommand();
  private CommandlineParser parser = new CommandlineParser(baseCommand, outPrintStream);
  private final AzureSubCommand azureSubCommand = new AzureSubCommand();

  @Before
  public void setup() {
    parser.registerSigner(azureSubCommand);
  }

  private Map<String, String> validStartupParameters() {
    final Map<String, String> params = new LinkedHashMap<>();
    params.put("--downstream-http-port", "7545");
    params.put("--chain-id", "1");
    params.put("--logging", "info");
    params.put("azure-signer", null);
    params.put("--keyvault-name", "ethsignertestkey");
    params.put("--key-name", "TestKey");
    params.put("--key-version", "449e655872f145a795f0849828685848");
    params.put("--client-id", "47efee5c-8079-4b48-96a7-31bb4f2e9ae2");
    params.put("--client-secret-path", createAzureSecretFile().getAbsolutePath());

    return params;
  }

  private <T> List<T> mapToString(final Map<T, T> input) {
    List<T> result = Lists.newArrayList();
    for (final Map.Entry<T, T> entry : input.entrySet()) {
      result.add(entry.getKey());
      if (entry.getValue() != null) {
        result.add(entry.getValue());
      }
    }
    return result;
  }

  @Test
  public void azureSigningStartsupCorrectlyAndShowsNoErrorsToSpecifiedOutput() {
    final List<String> params = mapToString(validStartupParameters());
    String[] cmdLineParams = params.toArray(new String[0]);

    parser.parseCommandLine(cmdLineParams);
    assertThat(commandOutput.size()).isEqualTo(0);
  }

  @Test
  public void inaccessibleKeyVaultNameDisplaysError() {
    final Map<String, String> paramsKeyValues = validStartupParameters();
    paramsKeyValues.replace("--keyvault-name", "invalid_key_vault");
    String[] cmdLineParams = mapToString(paramsKeyValues).toArray(new String[0]);

    parser.parseCommandLine(cmdLineParams);
    assertThat(commandOutput.toString()).startsWith(CommandlineParser.SIGNER_CREATION_ERROR);
  }

  @Test
  public void inaccessibleKeyInVaultDisplaysError() {
    final Map<String, String> paramsKeyValues = validStartupParameters();
    paramsKeyValues.replace("--key-name", "invalid_key_name");
    String[] cmdLineParams = mapToString(paramsKeyValues).toArray(new String[0]);

    parser.parseCommandLine(cmdLineParams);
    assertThat(commandOutput.toString()).startsWith(CommandlineParser.SIGNER_CREATION_ERROR);
  }

  @Test
  public void invalidSigninCredentialsDisplayError() {
    final Map<String, String> paramsKeyValues = validStartupParameters();
    paramsKeyValues.replace("--key-name", "invalid_key_name");
    String[] cmdLineParams = mapToString(paramsKeyValues).toArray(new String[0]);

    parser.parseCommandLine(cmdLineParams);
    assertThat(commandOutput.toString()).startsWith(CommandlineParser.SIGNER_CREATION_ERROR);
  }

  private File createAzureSecretFile() {
    return createTmpFile("azure_secret", "TW_3Uc/GLDdpLp5*om@MGcdlT29MuP*5".getBytes(UTF_8));
  }

  private File createTmpFile(final String tempNamePrefix, final byte[] data) {
    final Path path;
    try {
      path = Files.createTempFile(tempNamePrefix, null);
      Files.write(path, data);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final File tmpFile = path.toFile();
    tmpFile.deleteOnExit();
    return tmpFile;
  }
}

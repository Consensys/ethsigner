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
import static org.junit.Assert.fail;

import tech.pegasys.ethsigner.CommandLineConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import picocli.CommandLine;

@RunWith(MockitoJUnitRunner.class)
public class CommandLineConfigTest {

  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);

  private CommandLineConfig config = new CommandLineConfig(outPrintStream);

  private boolean parseCommand(final String cmdLine) {
    return false;
    // TODO(tmm): THIS NEEDS TO BE RECREATED COMPLETELY!
    //return config.parse(new MyHandler<List<Object>>(), cmdLine.split(" "));
  }

  private String validCommandLine() {
    return "--downstream-http-host=8.8.8.8 "
        + "--downstream-http-port=5000 "
        + "--downstream-http-request-timeout=10000 "
        + "--http-listen-port=5001 "
        + "--http-listen-host=localhost "
        + "--chain-id=6 "
        + "--logging=INFO";
  }

  private String removeFieldFrom(final String input, final String fieldname) {
    return input.replaceAll("--" + fieldname + "=.*?(\\s|$)", "");
  }

  private String modifyField(final String input, final String fieldname, final String value) {
    return input.replaceFirst("--" + fieldname + "=.*\\b", "--" + fieldname + "=" + value);
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() throws UnknownHostException {
    final boolean result = parseCommand(validCommandLine());

    assertThat(result).isTrue();
    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getDownstreamHttpHost()).isEqualTo(InetAddress.getByName("8.8.8.8"));
    assertThat(config.getDownstreamHttpPort()).isEqualTo(5000);
    assertThat(config.getDownstreamHttpRequestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.getHttpListenHost()).isEqualTo(InetAddress.getByName("localhost"));
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
  }

  @Test
  public void helpMessageIsShown() {
    parseCommand("--help");
    final String expectedOutputStart = String.format("Usage:%n%nethsigner [OPTIONS]");
    assertThat(commandOutput.toString()).startsWith(expectedOutputStart);
  }

  @Test
  public void nonIntegerInputForDownstreamPortShowsError() {
    final String cmdLine = modifyField(validCommandLine(), "downstream-http-port", "abc");
    try {
      parseCommand(cmdLine);
      fail();
    } catch (final Exception e) {
      assertThat(e.getCause().toString()).contains("--downstream-http-port", "'abc' is not an int");
    }
  }

  @Test
  public void missingRequiredParamShowsAppropriateError() {
    missingParameterShowsError("downstream-http-port");
  }

  @Test
  public void missingOptionalParametersAreSetToDefault() {
    // Must recreate config before executions, to prevent stale data remaining in the object.
    config = new CommandLineConfig(outPrintStream);
    missingOptionalParameterIsValidAndMeetsDefault("logging", config::getLogLevel, Level.INFO);

    config = new CommandLineConfig(outPrintStream);
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-host", config::getDownstreamHttpHost, InetAddress.getLoopbackAddress());

    config = new CommandLineConfig(outPrintStream);
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-request-timeout",
        config::getDownstreamHttpRequestTimeout,
        Duration.ofSeconds(5));

    config = new CommandLineConfig(outPrintStream);
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-port", config::getHttpListenPort, 8545);

    config = new CommandLineConfig(outPrintStream);
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-host", config::getHttpListenHost, InetAddress.getLoopbackAddress());
  }

  private void missingParameterShowsError(final String paramToRemove) {
    final String cmdLine = removeFieldFrom(validCommandLine(), paramToRemove);
    try {
      parseCommand(cmdLine);
      fail();
    } catch (final Exception e) {
      assertThat(e.getCause().toString()).contains("--" + paramToRemove, "Missing");
    }
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {
    final String cmdLine = removeFieldFrom(validCommandLine(), paramToRemove);
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }

  @Test
  public void domainNamesDecodeIntoAnInetAddress() {
    final String input =
        "--downstream-http-host=google.com "
            + "--downstream-http-port=5000 "
            + "--downstream-http-request-timeout=10000 "
            + "--http-listen-port=5001 "
            + "--http-listen-host=localhost "
            + "--chain-id=6 "
            + "--logging=INFO";

    final boolean result = parseCommand(input);
    assertThat(result).isTrue();
    assertThat(config.getDownstreamHttpHost().getHostName()).isEqualTo("google.com");
  }

  private static class MyHandler<R> implements CommandLine.IParseResultHandler2<R> {
    @Override
    public R handleParseResult(final CommandLine.ParseResult parseResult)
        throws CommandLine.ExecutionException {
      return null;
    }
  }
}

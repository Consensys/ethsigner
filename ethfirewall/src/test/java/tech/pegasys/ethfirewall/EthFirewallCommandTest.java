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
package tech.pegasys.ethfirewall;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.RunLast;

@RunWith(MockitoJUnitRunner.class)
public class EthFirewallCommandTest {
  final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);

  final ByteArrayOutputStream commandErrorOutput = new ByteArrayOutputStream();
  private final PrintStream errPrintStream = new PrintStream(commandErrorOutput);

  final EthFirewallCommand command = new EthFirewallCommand();

  private void parseCommand(String cmdLine) {
    command.parse(
        new RunLast().useOut(outPrintStream).useAnsi(Ansi.OFF),
        command.exceptionHandler().useErr(errPrintStream).useAnsi(Ansi.OFF),
        cmdLine.split(" "));
  }

  @Test
  public void helpMessageIsShown() {
    parseCommand("--help");
    final String expectedOutputStart = String.format("Usage:%n%nethfirewall [OPTIONS]");
    assertThat(commandOutput.toString()).startsWith(expectedOutputStart);
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void invalidInputForPortShowsError() {
    parseCommand(
        "--password=MyPassword --keyfile=./keyfile --downstream-http-host=127.0.0.1 --downstream-http-port=abc");
    assertThat(commandErrorOutput.toString())
        .contains("--downstream-http-port", "'abc' is not an int");
  }

  @Test
  public void missingPasswordShowsError() {
    parseCommand("--keyfile=./keyfile --downstream-host=127.0.0.1 --downstream-port=5000");
    assertThat(commandErrorOutput.toString()).contains("--password", "Missing");
  }

  @Test
  public void fieldsFromCommandLineAreStoredInVariables() {
    parseCommand(
        "--keyfile=./keyfile "
            + "--downstream-http-host=127.0.0.1 "
            + "--downstream-http-port=5000 "
            + "--http-listen-port=5001"
            + "--http-listen-host=localhost "
            + "--logging=TRACE");


  }
}

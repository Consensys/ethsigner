package tech.pegasys.ethfirewall;

import static org.assertj.core.api.Assertions.assertThat;
import static picocli.CommandLine.defaultExceptionHandler;

import com.sun.org.apache.bcel.internal.generic.ANEWARRAY;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import picocli.CommandLine.DefaultExceptionHandler;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.RunLast;

@RunWith(MockitoJUnitRunner.class)
public class EthFirewallCommandTest {

  @Mock private Logger logger;
  final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();
  private final PrintStream outPrintStream = new PrintStream(commandOutput);

  final ByteArrayOutputStream commandErrorOutput = new ByteArrayOutputStream();
  private final PrintStream errPrintStream = new PrintStream(commandErrorOutput);

  final EthFirewallCommand command = new EthFirewallCommand();

  private void parseCommand(String... args) {
    command.parse(
        outPrintStream,
        args);
  }

  @Test
  public void helpMessageIsShown() {
    parseCommand("--help");
    final String expectedOutputStart = String.format("Usage:%n%nethfirewall [OPTIONS]");
    assertThat(commandOutput.toString()).startsWith(expectedOutputStart);
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void callingVersionDisplaysEthFireWallVersionString() {

  }
}
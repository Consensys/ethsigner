/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.util;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.CmdlineHelpers.removeFieldFrom;

import tech.pegasys.ethsigner.CommandlineParser;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface CommandLineParserAssertions {
  static void parseCommandLineWithMissingParamsShowsError(
      final CommandlineParser parser,
      final ByteArrayOutputStream outputStream,
      final String defaultUsageText,
      final String inputCmdLine,
      final List<String> paramsToRemove) {
    final String cmdLine =
        removeFieldFrom(inputCmdLine, paramsToRemove.stream().toArray(String[]::new));
    final boolean result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).as("Parse Results After Removing Params").isFalse();

    final String output = outputStream.toString();
    final String patternStart = "(.*)Missing required (argument|option)(.*)(";
    final String patternMiddle =
        paramsToRemove.stream().map(s -> "(.*)--" + s + "(.*)").collect(Collectors.joining("|"));
    final String patternEnd = ")(.*)\\sUsage:";

    boolean isMatched =
        Pattern.compile(patternStart + patternMiddle + patternEnd).matcher(output).find();
    assertThat(isMatched).isTrue();

    assertThat(output).containsOnlyOnce(defaultUsageText);
  }
}

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
package tech.pegasys.ethsigner.valueprovider;

import static java.util.function.Predicate.not;
import static tech.pegasys.ethsigner.valueprovider.PrefixUtil.stripPrefix;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tuweni.toml.Toml;
import org.apache.tuweni.toml.TomlArray;
import org.apache.tuweni.toml.TomlParseError;
import org.apache.tuweni.toml.TomlParseResult;
import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

/** Toml Configuration which is specifically written for EthSigner and sub-commands. */
public class TomlConfigFileDefaultProvider implements IDefaultValueProvider {

  private final CommandLine commandLine;
  private final Path configFile;
  // this will be initialized on fist call of defaultValue by PicoCLI parseArgs
  private TomlParseResult result;

  public TomlConfigFileDefaultProvider(final CommandLine commandLine, final Path configFile) {
    this.commandLine = commandLine;
    this.configFile = configFile;
  }

  @Override
  public String defaultValue(final ArgSpec argSpec) {
    if (result == null) {
      result = loadConfigurationFromFile(configFile, commandLine);
      checkEmptyFile(result, configFile, commandLine);
      checkUnknownOptions();
    }

    // only options can be used in config because a name is needed for the key
    // so we skip default for positional params
    return argSpec.isOption() ? getConfigurationValue(((OptionSpec) argSpec)) : null;
  }

  private static TomlParseResult loadConfigurationFromFile(
      final Path configFile, final CommandLine commandLine) {
    try {
      final TomlParseResult result = Toml.parse(configFile);
      if (result.hasErrors()) {
        final String errors =
            result.errors().stream()
                .map(TomlParseError::toString)
                .collect(Collectors.joining("%n"));

        throw new ParameterException(
            commandLine, String.format("Invalid TOML configuration: %s", errors));
      }

      return result;
    } catch (final FileNotFoundException e) {
      throw new ParameterException(commandLine, "TOML configuration file not found: " + configFile);
    } catch (final IOException e) {
      throw new ParameterException(
          commandLine,
          "Unexpected IO error while reading TOML configuration file. " + e.getMessage());
    }
  }

  private void checkUnknownOptions() {
    final Set<String> picoCliOptionsKeys = new TreeSet<>();

    // parent command options
    final Set<String> mainCommandOptions =
        commandLine.getCommandSpec().options().stream()
            .map(TomlConfigFileDefaultProvider::buildOptionName)
            .collect(Collectors.toSet());

    // subcommands options
    final Set<String> subCommandsOptions =
        commandLine.getSubcommands().values().stream()
            .flatMap(TomlConfigFileDefaultProvider::subCommandOptions)
            .map(TomlConfigFileDefaultProvider::buildQualifiedOptionName)
            .collect(Collectors.toSet());

    picoCliOptionsKeys.addAll(mainCommandOptions);
    picoCliOptionsKeys.addAll(subCommandsOptions);

    final Set<String> unknownOptionsList =
        result.dottedKeySet().stream()
            .filter(not(picoCliOptionsKeys::contains))
            .collect(Collectors.toCollection(TreeSet::new));

    if (!unknownOptionsList.isEmpty()) {
      final String options = unknownOptionsList.size() > 1 ? "options" : "option";
      final String csvUnknownOptions = String.join(", ", unknownOptionsList);
      throw new ParameterException(
          commandLine,
          String.format("Unknown %s in TOML configuration file: %s", options, csvUnknownOptions));
    }
  }

  private static Stream<OptionSpec> subCommandOptions(final CommandLine subcommand) {
    return subcommand.getCommandSpec().options().stream();
  }

  private static String buildQualifiedOptionName(final OptionSpec optionSpec) {
    return optionSpec.command().name() + "." + buildOptionName(optionSpec);
  }

  private static void checkEmptyFile(
      final TomlParseResult result, final Path configFile, final CommandLine commandLine) {
    if (result == null || result.isEmpty()) {
      throw new ParameterException(
          commandLine, String.format("Empty TOML configuration file: %s", configFile));
    }
  }

  private String getConfigurationValue(final OptionSpec optionSpec) {
    final String keyName;
    if (commandLine.getCommandName().equals(optionSpec.command().name())) {
      keyName = buildOptionName(optionSpec);
    } else {
      // subcommand option
      keyName = buildQualifiedOptionName(optionSpec);
    }

    final Object value = result.get(keyName);

    if (value == null) {
      return null;
    }

    // handle array
    if (optionSpec.isMultiValue() && result.isArray(keyName)) {
      return ((TomlArray) value)
          .toList().stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    // convert all other values to string
    return String.valueOf(value);
  }

  private static String buildOptionName(final OptionSpec optionSpec) {
    return stripPrefix(optionSpec.longestName());
  }
}

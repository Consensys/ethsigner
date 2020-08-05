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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

/** Toml Configuration which is specifically written for EthSigner and sub-commands. */
public class TomlConfigFileDefaultProvider implements IDefaultValueProvider {

  private final CommandLine commandLine;
  private final File configFile;
  // this will be initialized on fist call of defaultValue by PicoCLI parseArgs
  private Map<String, Object> result;

  public TomlConfigFileDefaultProvider(final CommandLine commandLine, final File configFile) {
    this.commandLine = commandLine;
    this.configFile = configFile;
  }

  @Override
  public String defaultValue(final ArgSpec argSpec) {
    if (result == null) {
      result = loadConfigurationFromFile();
      checkConfigurationValidity(result == null || result.isEmpty());
      checkUnknownOptions(result);
    }

    // only options can be used in config because a name is needed for the key
    // so we skip default for positional params
    return argSpec.isOption() ? getConfigurationValue(((OptionSpec) argSpec)) : null;
  }

  private Map<String, Object> loadConfigurationFromFile() {
    return Collections.emptyMap(); // unreachable
  }

  /*
  @SuppressWarnings("AnnotateFormatMethod")
  private void throwParameterException(
          final Throwable cause, final String message, Object... messageArgs) {
    throw new ParameterException(
        commandLine,
        ArrayUtils.isEmpty(messageArgs) ? message : String.format(message, messageArgs),
        cause);
  }*/

  private void checkUnknownOptions(final Map<String, Object> result) {
    final CommandSpec commandSpec = commandLine.getCommandSpec();

    final Set<String> unknownOptionsList =
        result.keySet().stream()
            .filter(option -> !commandSpec.optionsMap().containsKey("--" + option))
            .collect(Collectors.toCollection(TreeSet::new));

    if (!unknownOptionsList.isEmpty()) {
      final String options = unknownOptionsList.size() > 1 ? "options" : "option";
      final String csvUnknownOptions = String.join(", ", unknownOptionsList);
      throw new ParameterException(
          commandLine,
          String.format("Unknown %s in toml configuration file: %s", options, csvUnknownOptions));
    }
  }

  private void checkConfigurationValidity(boolean isEmpty) {
    if (isEmpty) {
      throw new ParameterException(
          commandLine, String.format("Empty toml configuration file: %s", configFile));
    }
  }

  private String getConfigurationValue(final OptionSpec optionSpec) {
    final Optional<Object> optionalValue = getKeyName(optionSpec).map(result::get);
    if (optionalValue.isEmpty()) {
      return null;
    }

    final Object value = optionalValue.get();

    if (optionSpec.isMultiValue() && value instanceof Collection) {
      return ((Collection<?>) value).stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    return String.valueOf(value);
  }

  private Optional<String> getKeyName(final OptionSpec spec) {
    // If any of the names of the option are used as key in the toml results
    // then returns the value of first one.
    return Arrays.stream(spec.names())
        // remove leading dashes on option name as we can have "--" or "-" options
        .map(name -> name.replaceFirst("^-+", ""))
        .filter(result::containsKey)
        .findFirst();
  }
}

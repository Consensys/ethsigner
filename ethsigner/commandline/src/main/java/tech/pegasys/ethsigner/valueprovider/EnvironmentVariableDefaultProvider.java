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

import java.util.Map;

import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.OptionSpec;

public class EnvironmentVariableDefaultProvider implements IDefaultValueProvider {

  private final Map<String, String> environment;

  public EnvironmentVariableDefaultProvider(final Map<String, String> environment) {
    this.environment = environment;
  }

  @Override
  public String defaultValue(final ArgSpec argSpec) {

    if (argSpec.isOption()) {
      final OptionSpec optionSpec = (OptionSpec) argSpec;
      final String prefix = optionSpec.command().qualifiedName("_").toUpperCase() + "_";
      final String key =
          prefix + stripPrefix(optionSpec.longestName()).replace("-", "_").toUpperCase();
      return environment.get(key);
    }

    return null; // currently not supporting positional parameters
  }

  private static String stripPrefix(String prefixed) {
    for (int i = 0; i < prefixed.length(); i++) {
      if (Character.isJavaIdentifierPart(prefixed.charAt(i))) {
        return prefixed.substring(i);
      }
    }
    return prefixed;
  }
}

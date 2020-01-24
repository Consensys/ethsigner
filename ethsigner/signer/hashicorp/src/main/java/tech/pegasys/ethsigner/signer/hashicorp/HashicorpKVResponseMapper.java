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
package tech.pegasys.ethsigner.signer.hashicorp;

import tech.pegasys.ethsigner.TransactionSignerInitializationException;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;

class HashicorpKVResponseMapper {
  protected static final String ERROR_INVALID_JSON =
      "Invalid response returned from Hashicorp Vault";

  /**
   * Convert Hashicorp KV Version 2 Secret Engine JSON response to map of key/values.
   *
   * @param jsonResponse response from Hashicorp Vault wrapped in io.vertx.core.json.JsonObject
   * @return All key/value pairs returned from particular secret
   */
  static Map<String, String> extractMapFromJson(final JsonObject jsonResponse) {
    final Object outerData = jsonResponse.getValue("data");
    if (!(outerData instanceof JsonObject)) {
      throw new TransactionSignerInitializationException(ERROR_INVALID_JSON);
    }
    final JsonObject outerDataJsonObject = (JsonObject) outerData;

    // inner data contains multiple key/value pairs
    final Object innerData = outerDataJsonObject.getValue("data");
    if (!(innerData instanceof JsonObject)) {
      throw new TransactionSignerInitializationException(ERROR_INVALID_JSON);
    }
    final JsonObject keyData = (JsonObject) innerData;

    return Collections.unmodifiableMap(
        keyData.stream()
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(
                Collectors.toMap(Map.Entry::getKey, HashicorpKVResponseMapper::mapValueToString)));
  }

  private static String mapValueToString(final Map.Entry<String, Object> v) {
    return v.getValue().toString();
  }
}

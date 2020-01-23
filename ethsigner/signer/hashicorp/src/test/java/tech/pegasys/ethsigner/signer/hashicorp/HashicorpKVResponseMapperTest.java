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

import java.util.Map;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HashicorpKVResponseMapperTest {
  private static final String validKVEngineJsonResponse =
      " {\"data\": {\"data\": {"
          + "      \"foo\": \"bar\","
          + "      \"x\": \"y\"},"
          + "    \"metadata\": {"
          + "      \"created_time\": \"2018-03-22T02:24:06.945319214Z\","
          + "      \"deletion_time\": \"\","
          + "      \"destroyed\": false,"
          + "      \"version\": 1}}}";

  private static final String validJson = "{\"data\": \"test\"}";

  @Test
  void mapExtractedFromValidJson() {
    final JsonObject jsonObject = new JsonObject(validKVEngineJsonResponse);
    final Map<String, String> dataMap = HashicorpKVResponseMapper.extractMapFromJson(jsonObject);
    Assertions.assertEquals(2, dataMap.size());
    Assertions.assertEquals("y", dataMap.get("x"));
  }

  @Test
  void exceptionRaisedWhenJsonIsNotFromKVEngine() {
    final JsonObject jsonObject = new JsonObject(validJson);
    Assertions.assertThrows(
        TransactionSignerInitializationException.class,
        () -> HashicorpKVResponseMapper.extractMapFromJson(jsonObject));
  }
}

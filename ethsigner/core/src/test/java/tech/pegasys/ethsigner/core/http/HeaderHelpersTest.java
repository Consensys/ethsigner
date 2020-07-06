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
package tech.pegasys.ethsigner.core.http; /*
                                           * Copyright ConsenSys AG.
                                           *
                                           * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
                                           * the License. You may obtain a copy of the License at
                                           *
                                           * http://www.apache.org/licenses/LICENSE-2.0
                                           *
                                           * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
                                           * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
                                           * specific language governing permissions and limitations under the License.
                                           *
                                           * SPDX-License-Identifier: Apache-2.0
                                           */

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.google.common.net.HttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HeaderHelpersTest {

  @ParameterizedTest
  @ValueSource(strings = {"ORIGIN", "origin", "Origin", "OrIgIn"})
  void originHeadersAreRemoved(String headerToRemove) {
    final MultiMap input = new VertxHttpHeaders();
    input.add(headerToRemove, "arbitrary");
    final Map<String, String> output = HeaderHelpers.createHeaders(input);

    assertThat(output.keySet()).doesNotContain(headerToRemove);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CONTENT-LENGTH", "content-length", "Content-Length", "CoNtEnT-LeNgTh"})
  void contentLengthHeaderIsStripped(String headerToRemove) {
    final MultiMap input = new VertxHttpHeaders();
    input.add(headerToRemove, "arbitrary");
    final Map<String, String> output = HeaderHelpers.createHeaders(input);

    assertThat(output.keySet()).doesNotContain(headerToRemove);
  }

  @Test
  void hostHeaderIsRenamed() {
    final MultiMap input = new VertxHttpHeaders();
    input.add(HttpHeaders.HOST, "arbitrary");

    final Map<String, String> output = HeaderHelpers.createHeaders(input);

    assertThat(output.keySet()).doesNotContain(HttpHeaders.HOST);
    assertThat(output.get(HttpHeaders.X_FORWARDED_HOST)).isEqualTo("arbitrary");
  }
}

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
package tech.pegasys.ethsigner.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HexStringComparatorTest {

  final HexStringComparator comparator = new HexStringComparator();

  @ParameterizedTest
  @ValueSource(strings = {"05", "0x05"})
  void existenceOf0xdoesntAffectcomparison(String otherValue) {
    assertThat(comparator.compare("06", otherValue)).isEqualTo(1);
    assertThat(comparator.compare(otherValue, "06")).isEqualTo(-1);

    assertThat(comparator.compare("05", otherValue)).isEqualTo(0);
    assertThat(comparator.compare("0x05", otherValue)).isEqualTo(0);
    assertThat(comparator.compare(otherValue, "05")).isEqualTo(0);
    assertThat(comparator.compare(otherValue, "0x05")).isEqualTo(0);
  }

  @Test
  void nonHexStringThrowsException() {
    final Throwable thrown = catchThrowable(() -> comparator.compare("NotHex", "0x01"));
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }
}

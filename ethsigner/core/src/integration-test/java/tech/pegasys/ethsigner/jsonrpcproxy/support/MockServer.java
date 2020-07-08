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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.ethsigner.jsonrpcproxy.support;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map.Entry;
import org.mockserver.model.Header;
import org.mockserver.model.Headers;

public class MockServer {

  public static Headers headers(final Iterable<Entry<String, String>> headers) {
    final List<Header> result = Lists.newArrayList();
    headers.forEach(entry -> result.add(new Header(entry.getKey(), entry.getValue())));
    return new Headers(result);
  }
}

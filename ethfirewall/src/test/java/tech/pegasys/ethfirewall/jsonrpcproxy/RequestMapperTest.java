/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.ethfirewall.jsonrpcproxy;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestMapperTest {
  @Mock private Handler<RoutingContext> defaultHandler;
  @Mock private Handler<RoutingContext> handler1;
  @Mock private Handler<RoutingContext> handler2;

  @Test
  public void returnsHandleForAssociatedRpcMethod() {
    RequestMapper requestMapper = new RequestMapper(defaultHandler);
    requestMapper.addHandler("foo", handler1);
    requestMapper.addHandler("bar", handler2);
    requestMapper.addHandler("default", defaultHandler);
    assertThat(requestMapper.getMatchingHandler(rpcJson("foo"))).isSameAs(handler1);
    assertThat(requestMapper.getMatchingHandler(rpcJson("bar"))).isSameAs(handler2);
  }

  @Test
  public void returnsDefaultHandlerForUnknownRpcMethod() {
    RequestMapper requestMapper = new RequestMapper(defaultHandler);
    requestMapper.addHandler("unknown", defaultHandler);
    requestMapper.addHandler("", defaultHandler);
  }

  private JsonObject rpcJson(final String methodName) {
    return new JsonObject(ImmutableMap.of("method", methodName));
  }
}

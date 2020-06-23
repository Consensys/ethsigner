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
package tech.pegasys.ethsigner.core.http;

import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static io.vertx.core.http.HttpHeaders.ORIGIN;

import java.util.List;

import com.google.common.collect.Lists;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class StripCorsHeadersHandler implements Handler<RoutingContext> {

  private static final List<CharSequence> headersToRemove =
      Lists.newArrayList(ORIGIN, ACCESS_CONTROL_ALLOW_METHODS, ACCESS_CONTROL_ALLOW_HEADERS);

  @Override
  public void handle(final RoutingContext context) {
    headersToRemove.forEach(headerName -> context.request().headers().remove(headerName));
    context.next();
  }
}

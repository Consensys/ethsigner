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
package tech.pegasys.ethsigner.jsonrpcproxy;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Failure handler that records log details of the problem. */
public class LogErrorHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(LogErrorHandler.class);

  @Override
  public void handle(final RoutingContext failureContext) {

    if (failureContext.failed()) {
      LOG.error(
          String.format(
              "Failed sendRequest: %s %s",
              failureContext.request().absoluteURI(), failureContext.getBodyAsString()),
          failureContext.failure());
      // Let the next matching route or error handler deal with the error, we only handle logging
      failureContext.next();
    } else {
      LOG.warn("Error handler triggered without any propagated failure");
    }
  }
}

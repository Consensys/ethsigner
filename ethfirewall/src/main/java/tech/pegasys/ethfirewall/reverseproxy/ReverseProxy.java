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
package tech.pegasys.ethfirewall.reverseproxy;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReverseProxy {

  private static final Logger LOG = LoggerFactory.getLogger(ReverseProxy.class);

  /** The destination for proxy requests. */
  private final HttpClient target;

  public ReverseProxy(final HttpClient target) {
    this.target = target;
  }

  /**
   * Forwards the sendRequest onto the target, writing the response from the target to the original
   * sendRequest.
   *
   * @param context contains the sendRequest for proxy.
   */
  public void proxy(final RoutingContext context) {
    final HttpServerRequest originalRequest = context.request();
    final HttpClientRequest proxyRequest =
        target.request(
            originalRequest.method(),
            originalRequest.uri(),
            proxiedResponse -> {
              logResponse(proxiedResponse);

              originalRequest.response().setStatusCode(proxiedResponse.statusCode());
              originalRequest.response().headers().setAll(proxiedResponse.headers());
              originalRequest.response().setChunked(true);

              proxiedResponse.bodyHandler(
                  data -> {
                    logResponseBody(data);

                    // End the sendRequest, preventing any other handler from executing
                    originalRequest.response().end(data);
                  });
            });

    proxyRequest.headers().setAll(originalRequest.headers());
    proxyRequest.setChunked(true);

    logRequest(context, proxyRequest);

    // Ends the sendRequest, completing execution of the proxy call
    proxyRequest.end(context.getBody());
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  private void logRequest(final RoutingContext context, final HttpClientRequest proxyRequest) {
    LOG.debug(
        "Proxying originalRequest: method: {}, uri: {}, body: {}, target: method: {}, uri: {}",
        context.request().method(),
        context.request().absoluteURI(),
        context.getBody(),
        proxyRequest.method(),
        proxyRequest.absoluteURI());
  }
}

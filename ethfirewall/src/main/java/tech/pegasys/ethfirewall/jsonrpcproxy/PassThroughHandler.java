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

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassThroughHandler implements RequestHandler {
  private static final Logger LOG = LoggerFactory.getLogger(PassThroughHandler.class);
  private final HttpClient ethNodeClient;

  public PassThroughHandler(final HttpClient ethNodeClient) {
    this.ethNodeClient = ethNodeClient;
  }

  @Override
  public Future<Response> handle(final Request request) {
    final Future<Response> futureResponse = Future.future();
    final HttpClientRequest proxyRequest =
        ethNodeClient.request(
            request.getMethod(),
            request.getUri(),
            proxiedResponse -> {
              logResponse(proxiedResponse);
              proxiedResponse.bodyHandler(
                  data -> {
                    logResponseBody(data);
                    final Response response =
                        new Response(proxiedResponse.statusCode(), proxiedResponse.headers(), data);
                    futureResponse.complete(response);
                  });
            });

    proxyRequest.headers().setAll(request.getHeaders());
    proxyRequest.headers().remove("Content-Length"); // created during 'end'.
    proxyRequest.setChunked(false);

    // Ends the sendRequest, completing execution of the proxy call
    proxyRequest.end(request.getBody());

    logRequest(request, proxyRequest);
    return futureResponse;
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  private void logRequest(final Request request, final HttpClientRequest proxyRequest) {
    LOG.debug(
        "Send Request downstream: method: {}, uri: {}, body: {}, ethNodeClient: method: {}, uri: {}",
        request.getMethod(),
        request.getUri(),
        request.getBody(),
        proxyRequest.method(),
        proxyRequest.absoluteURI());
  }
}

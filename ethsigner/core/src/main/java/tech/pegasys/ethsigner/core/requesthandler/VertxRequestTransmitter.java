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
package tech.pegasys.ethsigner.core.requesthandler;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;

import com.google.common.net.HttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VertxRequestTransmitter {

  private static final Logger LOG = LogManager.getLogger();
  private final Duration httpRequestTimeout;
  private final ResponseBodyHandler bodyHandler;

  public VertxRequestTransmitter(
      final Duration httpRequestTimeout, final ResponseBodyHandler bodyHandler) {
    this.httpRequestTimeout = httpRequestTimeout;
    this.bodyHandler = bodyHandler;
  }

  private void handleException(final RoutingContext context, final Throwable thrown) {
    if (thrown instanceof TimeoutException || thrown instanceof ConnectException) {
      context.fail(GATEWAY_TIMEOUT.code(), thrown);
    } else if (thrown instanceof SSLHandshakeException) {
      context.fail(BAD_GATEWAY.code(), thrown);
    } else {
      context.fail(INTERNAL_SERVER_ERROR.code(), thrown);
    }
  }

  public void handleResponse(final RoutingContext context, final HttpClientResponse response) {
    logResponse(response);

    response.bodyHandler(
        body ->
            context
                .vertx()
                .executeBlocking(
                    future -> {
                      logResponseBody(body);
                      bodyHandler.handleResponseBody(context, response, body);
                      future.complete();
                    },
                    false,
                    res -> {
                      if (res.failed()) {
                        LOG.error(
                            "An unhandled error occurred while processing "
                                + context.getBodyAsString(),
                            res.cause());
                        context.fail(res.cause());
                      }
                    }));
  }

  public void sendRequest(
      final HttpClientRequest request, final Buffer bodyContent, final RoutingContext context) {
    request.setTimeout(httpRequestTimeout.toMillis());
    request.exceptionHandler(thrown -> handleException(context, thrown));
    final MultiMap requestHeaders = createHeaders(context.request().headers());
    request.headers().setAll(requestHeaders);
    request.setChunked(false);
    request.end(bodyContent);
  }

  private MultiMap createHeaders(final MultiMap headers) {
    final MultiMap requestHeaders = new VertxHttpHeaders();
    requestHeaders.addAll(headers);
    requestHeaders.remove(HttpHeaders.CONTENT_LENGTH);
    requestHeaders.remove(HttpHeaders.ORIGIN);
    renameHeader(requestHeaders, HttpHeaders.HOST, HttpHeaders.X_FORWARDED_HOST);
    return requestHeaders;
  }

  private void renameHeader(
      final MultiMap headers, final String oldHeader, final String newHeader) {
    final String oldHeaderValue = headers.get(oldHeader);
    headers.remove(oldHeader);
    if (oldHeaderValue != null) {
      headers.add(newHeader, oldHeaderValue);
    }
  }

  private void logResponse(final HttpClientResponse response) {
    LOG.debug("Response status: {}", response.statusCode());
  }

  private void logResponseBody(final Buffer body) {
    LOG.debug("Response body: {}", body);
  }

  @FunctionalInterface
  public interface ResponseBodyHandler {

    void handleResponseBody(
        final RoutingContext context, final HttpClientResponse response, final Buffer body);
  }
}

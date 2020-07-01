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

import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.DownstreamPathCalculator;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;

import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VertxRequestTransmitter {

  private static final Logger LOG = LogManager.getLogger();
  private final Duration httpRequestTimeout;
  private final ResponseBodyHandler bodyHandler;
  private final HttpClient downStreamConnection;
  private final DownstreamPathCalculator downstreamPathCalculator;

  public VertxRequestTransmitter(
      final HttpClient downStreamConnection,
      final Duration httpRequestTimeout,
      final DownstreamPathCalculator downstreamPathCalculator,
      final ResponseBodyHandler bodyHandler) {
    this.httpRequestTimeout = httpRequestTimeout;
    this.bodyHandler = bodyHandler;
    this.downStreamConnection = downStreamConnection;
    this.downstreamPathCalculator = downstreamPathCalculator;
  }

  private void handleException(final Throwable thrown) {
    if (thrown instanceof TimeoutException || thrown instanceof ConnectException) {
      bodyHandler.handleTransmissionFailure(GATEWAY_TIMEOUT, thrown);
    } else if (thrown instanceof SSLHandshakeException) {
      bodyHandler.handleTransmissionFailure(BAD_GATEWAY, thrown);
    } else {
      bodyHandler.handleTransmissionFailure(INTERNAL_SERVER_ERROR, thrown);
    }
  }

  public void handleResponse(final HttpClientResponse response) {
    logResponse(response);
    response.bodyHandler(body -> bodyHandler.handleResponseBody(response, body));
  }

  public void sendRequest(
      final Buffer bodyContent,
      final String path,
      final HttpMethod method,
      final MultiMap headers) {
    final String fullPath = downstreamPathCalculator.calculateDownstreamPath(path);
    final HttpClientRequest request =
        downStreamConnection.request(method, fullPath, this::handleResponse);

    request.setTimeout(httpRequestTimeout.toMillis());
    request.exceptionHandler(this::handleException);
    final MultiMap requestHeaders = createHeaders(headers);
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

  public interface ResponseBodyHandler {

    void handleResponseBody(final HttpClientResponse response, final Buffer body);

    void handleTransmissionFailure(final HttpResponseStatus status, final Throwable t);
  }
}

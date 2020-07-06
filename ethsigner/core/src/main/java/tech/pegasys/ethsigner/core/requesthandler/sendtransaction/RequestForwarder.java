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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import tech.pegasys.ethsigner.core.requesthandler.DownstreamResponseHandler;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;

import com.google.common.net.HttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public abstract class RequestForwarder implements DownstreamResponseHandler {

  private final RoutingContext context;

  public RequestForwarder(final RoutingContext context) {
    this.context = context;
  }

  public abstract void send();

  @Override
  public void handleResponseBody(
      final Map<String, String> headers, final int statusCode, final String body) {
    context.response().setStatusCode(statusCode);
    context.response().headers().addAll(headers);
    context.response().setChunked(false);
    context.response().end(body);
  }

  @Override
  public void handleTransmissionFailure(final Throwable thrown) {
    if (thrown instanceof TimeoutException || thrown instanceof ConnectException) {
      context.fail(GATEWAY_TIMEOUT.code(), thrown);
    } else if (thrown instanceof SSLHandshakeException) {
      context.fail(BAD_GATEWAY.code(), thrown);
    } else {
      context.fail(INTERNAL_SERVER_ERROR.code(), new RuntimeException(thrown));
    }
  }

  protected Map<String, String> createHeaders(final MultiMap headers) {
    final Map<String, String> headersToSend = new HashMap<>();
    headers.forEach(entry -> headersToSend.put(entry.getKey(), entry.getValue()));
    headersToSend.remove(HttpHeaders.CONTENT_LENGTH);
    headersToSend.remove(HttpHeaders.ORIGIN);
    renameHeader(headersToSend, HttpHeaders.HOST, HttpHeaders.X_FORWARDED_HOST);
    return headersToSend;
  }

  private void renameHeader(
      final Map<String, String> headers, final String oldHeader, final String newHeader) {
    final String oldHeaderValue = headers.get(oldHeader);
    headers.remove(oldHeader);
    if (oldHeaderValue != null) {
      headers.put(newHeader, oldHeaderValue);
    }
  }

  protected RoutingContext context() {
    return context;
  }
}

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
package tech.pegasys.ethfirewall.jsonrpc;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReverseProxyHandlerTest {
  @Mock private HttpServerRequest serverRequest;
  @Mock private HttpServerResponse serverResponse;
  @Mock private HttpClientRequest clientRequest;
  @Mock private HttpClientResponse clientResponse;
  @Mock private HttpClient client;
  @Mock private RoutingContext context;

  private MultiMap clientReqHeaders = new VertxHttpHeaders();
  private MultiMap serverRespHeaders = new VertxHttpHeaders();
  private ReverseProxyHandler reverseProxyHandler;

  @Before
  public void setup() {
    reverseProxyHandler = new ReverseProxyHandler(client);

    final MultiMap headers = new VertxHttpHeaders();
    headers.add(CONTENT_LENGTH, "53");
    headers.set(CONTENT_TYPE, APPLICATION_JSON);

    when(context.request()).thenReturn(serverRequest);
    when(serverRequest.headers()).thenReturn(headers);
    when(serverRequest.response()).thenReturn(serverResponse);
    when(clientRequest.headers()).thenReturn(clientReqHeaders);
    when(clientResponse.headers()).thenReturn(headers);
    when(serverResponse.headers()).thenReturn(serverRespHeaders);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void proxiesRequest() {
    final Buffer serverReqBody = new BufferImpl();
    serverReqBody.appendString(
        "{\"jsonrpc\":\"2.0\",\"method\":\"net_version\",\"params\":[],\"id\":1}");
    final Buffer clientRespBody = new BufferImpl();
    clientRespBody.appendString("{\"jsonrpc\" : \"2.0\", \"id\" : 1, \"result\" : \"4\"}");

    when(context.getBody()).thenReturn(serverReqBody);
    when(serverRequest.method()).thenReturn(POST);
    when(serverRequest.uri()).thenReturn("/rpcs");
    when(client.request(any(), anyString(), any(Handler.class))).thenReturn(clientRequest);
    reverseProxyHandler.handle(context);

    // verify downstream req is made using server req details
    ArgumentCaptor<Handler<HttpClientResponse>> responseHandler =
        ArgumentCaptor.forClass(Handler.class);
    verify(client).request(eq(POST), eq("/rpcs"), responseHandler.capture());
    verify(clientRequest).end(serverReqBody);
    assertThat(clientReqHeaders).hasSize(2);
    assertThat(clientReqHeaders.get(CONTENT_LENGTH)).isEqualTo("53");
    assertThat(clientReqHeaders.get(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.toString());

    // verify client response is mapped back to server response
    when(clientResponse.statusCode()).thenReturn(200);
    responseHandler.getValue().handle(clientResponse);
    ArgumentCaptor<Handler<Buffer>> bodyHandler = ArgumentCaptor.forClass(Handler.class);
    verify(serverResponse).setChunked(true);
    verify(serverResponse).setStatusCode(200);
    verify(clientResponse).bodyHandler(bodyHandler.capture());
    assertThat(serverRespHeaders).hasSize(2);
    assertThat(serverRespHeaders.get(CONTENT_LENGTH)).isEqualTo("53");
    assertThat(serverRespHeaders.get(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.toString());

    // verify body content is mapped back
    bodyHandler.getValue().handle(clientRespBody);
    verify(serverResponse).end(clientRespBody);
  }
}

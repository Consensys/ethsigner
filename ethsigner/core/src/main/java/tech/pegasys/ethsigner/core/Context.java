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
package tech.pegasys.ethsigner.core;

import tech.pegasys.ethsigner.core.http.HttpResponseFactory;
import tech.pegasys.ethsigner.core.jsonrpc.JsonDecoder;
import tech.pegasys.ethsigner.core.signing.TransactionSignerProvider;

import java.nio.file.Path;
import java.time.Duration;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;

public class Context {

  private final long chainId;
  private final TransactionSignerProvider transactionSignerProvider;
  private final HttpClientOptions clientOptions;
  private final Duration httpRequestTimeout;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();
  private final JsonDecoder jsonDecoder;
  private final Path dataPath;
  private final Vertx vertx;
  private final HttpServerOptions serverOptions;

  public Context(
      final long chainId,
      final TransactionSignerProvider transactionSignerProvider,
      final HttpClientOptions clientOptions,
      final HttpServerOptions serverOptions,
      final Duration httpRequestTimeout,
      final JsonDecoder jsonDecoder,
      final Path dataPath,
      final Vertx vertx) {

    this.chainId = chainId;

    this.transactionSignerProvider = transactionSignerProvider;
    this.clientOptions = clientOptions;
    this.httpRequestTimeout = httpRequestTimeout;
    this.jsonDecoder = jsonDecoder;
    this.dataPath = dataPath;
    this.serverOptions = serverOptions;

    this.vertx = vertx;
  }

  public long getChainId() {
    return chainId;
  }

  public TransactionSignerProvider getTransactionSignerProvider() {
    return transactionSignerProvider;
  }

  public HttpClientOptions getClientOptions() {
    return clientOptions;
  }

  public Duration getHttpRequestTimeout() {
    return httpRequestTimeout;
  }

  public HttpResponseFactory getResponseFactory() {
    return responseFactory;
  }

  public JsonDecoder getJsonDecoder() {
    return jsonDecoder;
  }

  public Path getDataPath() {
    return dataPath;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public HttpServerOptions getServerOptions() {
    return serverOptions;
  }
}

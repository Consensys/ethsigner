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
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.ethsigner.core.EthSigner;
import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransactionFactoryTest {

  private Object privateParams;
  private Object publicParams;

  private JsonRpcRequest privateRequest;
  private JsonRpcRequest publicRequest;
  RoutingContext context;

  @BeforeEach
  public void setup() {

    privateParams =
        new Object() {
          public String from = "0x7577919ae5df4941180eac211965f275cdce314d";
          public String privateFrom = "ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI=";
          public String restriction = "restricted";
          public String to = "0xd46e8dd67c5d32be8058bb8eb970870f07244567";
          public String gas = "0x76c0";
          public String gasPrice = "0x9184e72a000";
          public String value = "0x0";
          public String nonce = "0x1";
          public String data =
              "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675";
          public String[] privateFor =
              new String[] {"GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w="};
        };

    privateRequest = new JsonRpcRequest("2.0", "eth_sendTransaction");
    privateRequest.setParams(privateParams);

    publicParams =
        new Object() {
          public String from = "0x7577919ae5df4941180eac211965f275cdce314d";
          public String to = "0xd46e8dd67c5d32be8058bb8eb970870f07244567";
          public String gas = "0x76c0";
          public String gasPrice = "0x9184e72a000";
          public String value = "0x0";
          public String nonce = "0x1";
          public String data =
              "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675";
        };

    publicRequest = new JsonRpcRequest("2.0", "eth_sendTransaction");
    publicRequest.setParams(publicParams);

    context = mock(RoutingContext.class);
    final HttpServerRequest request = mock(HttpServerRequest.class);
    final MultiMap headers = mock(MultiMap.class);

    when(context.request()).thenReturn(request);
    when(request.headers()).thenReturn(headers);
  }

  @Test
  public void convertsEthPrivateTransactionsToEeaWhenAsked() {
    final TransactionFactory factory =
        new TransactionFactory(EthSigner.createJsonDecoder(), null, true);

    Transaction privateTx = factory.createTransaction(context, privateRequest);
    assertThat(privateTx).isInstanceOf(EeaPrivateTransaction.class);

    Transaction publicTx = factory.createTransaction(context, publicRequest);
    assertThat(publicTx).isInstanceOf(EthTransaction.class);
  }

  @Test
  public void doesNotConvertEthPrivateTransactionsToEeaWhenAskedNotTo() {
    final TransactionFactory factory =
        new TransactionFactory(EthSigner.createJsonDecoder(), null, false);

    Transaction privateTx = factory.createTransaction(context, privateRequest);
    assertThat(privateTx).isInstanceOf(EthTransaction.class);

    Transaction publicTx = factory.createTransaction(context, publicRequest);
    assertThat(publicTx).isInstanceOf(EthTransaction.class);
  }
}

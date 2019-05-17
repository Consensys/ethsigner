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
package tech.pegasys.ethsigner.jsonrpcproxy.model.jsonrpc;

import io.vertx.core.json.Json;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.eea.Eea;

public class EeaSendRawTransaction {

  private final Eea eeaJsonRpc;

  public EeaSendRawTransaction(final Eea eeaJsonRpc) {
    this.eeaJsonRpc = eeaJsonRpc;
  }

  public String request() {
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        eeaJsonRpc.eeaSendRawTransaction(
            "0xf90110a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456780a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a00b528cefb87342b2097318cd493d13b4c9bd55bf35bf1b3cf2ef96ee14cee563a06423107befab5530c42a2d7d2590b96c04ee361c868c138b054d7886966121a6aa307837353737393139616535646634393431313830656163323131393635663237356364636533313464ebaa3078643436653864643637633564333262653830353862623865623937303837306630373234343536378a72657374726963746564");
    sendRawTransactionRequest.setId(77);

    return Json.encode(sendRawTransactionRequest);
  }

  public String request(final String value) {
    final Request<?, ? extends Response<?>> sendRawTransactionRequest =
        eeaJsonRpc.eeaSendRawTransaction(value);
    sendRawTransactionRequest.setId(77);

    return Json.encode(sendRawTransactionRequest);
  }

  public String response(final String value) {
    final Response<String> sendRawTransactionResponse = new EthSendTransaction();
    sendRawTransactionResponse.setResult(value);
    return Json.encode(sendRawTransactionResponse);
  }
}

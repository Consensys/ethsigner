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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;

public class EthProtocolVersion {

  final Request<?, org.web3j.protocol.core.methods.response.EthProtocolVersion> jsonRpcRequest;
  final String ethProtocolVersionRequest;

  public EthProtocolVersion(final Web3j web3j) {
    jsonRpcRequest = web3j.ethProtocolVersion();
    ethProtocolVersionRequest = Json.encode(jsonRpcRequest);
  }

  public String getEncodedRequestBody() {
    return ethProtocolVersionRequest;
  }

  public long getId() {
    return jsonRpcRequest.getId();
  }
}

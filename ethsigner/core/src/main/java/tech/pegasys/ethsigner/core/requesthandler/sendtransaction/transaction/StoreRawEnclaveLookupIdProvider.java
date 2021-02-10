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

import tech.pegasys.ethsigner.core.jsonrpc.JsonRpcRequest;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.EnclaveLookupIdProvider;

public class StoreRawEnclaveLookupIdProvider implements EnclaveLookupIdProvider {

  private final VertxStoreRawRequestTransmitter vertxStoreRawRequestTransmitter;

  public StoreRawEnclaveLookupIdProvider(
      final VertxStoreRawRequestTransmitter vertxStoreRawRequestTransmitter) {
    this.vertxStoreRawRequestTransmitter = vertxStoreRawRequestTransmitter;
  }

  @Override
  public String getLookupId(final String payload) {
    final JsonRpcRequest request = generateRequest(payload);
    return vertxStoreRawRequestTransmitter.storeRaw(request);
  }

  protected JsonRpcRequest generateRequest(final String payload) {
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "goquorum_storeRaw");
    request.setParams(new Object[] {payload});

    return request;
  }
}

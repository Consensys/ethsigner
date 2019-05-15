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
package tech.pegasys.ethsigner.jsonrpc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.vertx.core.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EeaSendTransactionJsonParameters extends EthSendTransactionJsonParameters {

  private String privateFrom;
  private List<String> privateFor;
  private String restriction;

  @JsonSetter("privateFrom")
  public void privateFrom(final String privateFrom) {
    this.privateFrom = privateFrom;
  }

  @JsonSetter("privateFor")
  public void privateFor(final List<String> privateFor) {
    this.privateFor = privateFor;
  }

  @JsonSetter("restriction")
  public void restriction(final String restriction) {
    this.restriction = restriction;
  }

  // TODO add fields to constructor
  @JsonCreator
  public EeaSendTransactionJsonParameters(@JsonProperty("from") final String sender) {
    super(sender);
  }

  public String privateFrom() {
    return privateFrom;
  }

  public List<String> privateFor() {
    return privateFor;
  }

  public String restriction() {
    return restriction;
  }

  // TODO lot of dupe with EthSendTransactionJsonParameters.from(JsonRpcRequest)
  public static EeaSendTransactionJsonParameters from(final JsonRpcRequest request) {

    final Object sendTransactionObject;
    final Object params = request.getParams();
    if (params instanceof List) {
      @SuppressWarnings("unchecked")
      final List<Object> paramList = (List<Object>) params;
      if (paramList.size() != 1) {
        throw new IllegalArgumentException(
            "SendTransaction Json Rpc requires a single parameter, request contained "
                + paramList.size());
      }
      sendTransactionObject = paramList.get(0);
    } else {
      sendTransactionObject = params;
    }

    final JsonObject receivedParams = JsonObject.mapFrom(sendTransactionObject);

    return receivedParams.mapTo(EeaSendTransactionJsonParameters.class);
  }
}

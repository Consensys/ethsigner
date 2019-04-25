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
package tech.pegasys.ethsigner.requesthandler.sendtransaction;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.ethsigner.jsonrpc.SendTransactionJsonParameters;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.web3j.crypto.RawTransaction;

public class RawTransactionBuilderTest {

  @Test
  public void ensureDefaultValuesArePopulatedForMissingFields() {
    final Map<String, Object> inputData = new HashMap<>();
    inputData.put("from", "0xABCDE");
    inputData.put("data", "TheData");

    final JsonObject input = new JsonObject(inputData);
    final SendTransactionJsonParameters params = input.mapTo(SendTransactionJsonParameters.class);

    final RawTransactionBuilder builder = RawTransactionBuilder.from(params);
    final RawTransaction rawTransaction = builder.build();

    assertThat(rawTransaction.getGasLimit()).isEqualTo(BigInteger.valueOf(90000));
    assertThat(rawTransaction.getGasPrice()).isEqualTo(BigInteger.ZERO);
    assertThat(rawTransaction.getValue()).isEqualTo(BigInteger.ZERO);
    assertThat(rawTransaction.getData()).isEqualTo(inputData.get("data"));
    assertThat(rawTransaction.getNonce()).isNull();
  }
}

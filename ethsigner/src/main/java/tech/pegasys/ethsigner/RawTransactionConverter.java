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
package tech.pegasys.ethsigner;

import tech.pegasys.ethsigner.jsonrpc.SendTransactionJsonParameters;

import java.math.BigInteger;
import java.util.Optional;

import org.web3j.crypto.RawTransaction;

public class RawTransactionConverter {

  public RawTransaction from(final SendTransactionJsonParameters input) {
    return RawTransaction.createTransaction(
        valueOrDefault(input.nonce(), null),
        valueOrDefault(input.gasPrice(), BigInteger.ZERO),
        valueOrDefault(input.gas(), BigInteger.valueOf(90000)),
        valueOrDefault(input.receiver(), ""),
        valueOrDefault(input.value(), BigInteger.ZERO),
        input.data());
  }

  private <T> T valueOrDefault(Optional<T> value, final T defaultValue) {
    if (value.isPresent()) {
      return value.get();
    }

    if (defaultValue != null) {
      return defaultValue;
    } else {
      throw new RuntimeException("Unable to have a default value of null.");
    }
  }
}

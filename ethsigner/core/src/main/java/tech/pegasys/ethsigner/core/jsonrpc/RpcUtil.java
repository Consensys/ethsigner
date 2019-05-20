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
package tech.pegasys.ethsigner.core.jsonrpc;

import java.math.BigInteger;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class RpcUtil {
  private static final String ENCODING_PREFIX = "0x";
  private static final int HEXADECIMAL = 16;
  private static final int HEXADECIMAL_PREFIX_LENGTH = 2;

  public static void validatePrefix(final String value) {
    if (!value.startsWith(ENCODING_PREFIX)) {
      throw new IllegalArgumentException(
          String.format("Prefix of '0x' is expected in value: %s", value));
    }
  }

  public static BigInteger optionalHex(final String value) {
    validatePrefix(value);
    return hex(value.substring(HEXADECIMAL_PREFIX_LENGTH));
  }

  private static BigInteger hex(final String value) {
    return new BigInteger(value, HEXADECIMAL);
  }

  public static <T> T fromRpcRequestToJsonParam(final Class<T> type, final JsonRpcRequest request) {

    final Object object;
    final Object params = request.getParams();
    if (params instanceof List) {
      @SuppressWarnings("unchecked")
      final List<Object> paramList = (List<Object>) params;
      if (paramList.size() != 1) {
        throw new IllegalArgumentException(
            type.getSimpleName()
                + " json Rpc requires a single parameter, request contained "
                + paramList.size());
      }
      object = paramList.get(0);
    } else {
      object = params;
    }

    final JsonObject receivedParams = JsonObject.mapFrom(object);

    return receivedParams.mapTo(type);
  }
}

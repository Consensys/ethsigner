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

public class CmdlineHelpers {

  public static String validBaseCommandOptions() {
    return "--downstream-http-host=8.8.8.8 "
        + "--downstream-http-port=5000 "
        + "--downstream-http-request-timeout=10000 "
        + "--http-listen-port=5001 "
        + "--http-listen-host=localhost "
        + "--chain-id=6 "
        + "--logging=INFO "
        + "--tls-keystore-file=./keystore.cert "
        + "--tls-keystore-password-file=./keystore.passwd "
        + "--tls-client-whitelist-file=./client_whitelist ";
  }

  public static String removeFieldFrom(final String input, final String fieldname) {
    return input.replaceAll("--" + fieldname + "=.*?(\\s|$)", "");
  }

  public static String modifyField(final String input, final String fieldname, final String value) {
    return input.replaceFirst("--" + fieldname + "=[^\\s]*", "--" + fieldname + "=" + value);
  }
}

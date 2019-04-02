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
package tech.pegasys.ethfirewall;

public class EthFirewallInfo {
  private static final String CLIENT_IDENTITY = "ethfirewall" + "";
  private static final String VERSION =
      CLIENT_IDENTITY
          + "/v"
          + EthFirewallInfo.class.getPackage().getImplementationVersion()
          + "/"
          + PlatformDetector.getOS()
          + "/"
          + PlatformDetector.getVM();

  private EthFirewallInfo() {}

  public static String clientIdentity() {
    return CLIENT_IDENTITY;
  }

  public static String version() {
    return VERSION;
  }
}

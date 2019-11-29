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
package tech.pegasys.ethsigner.signer.multiplatform;

public enum SignerType {
  FILE_BASED_SIGNER("file-based-signer"),
  UNKNOWN_TYPE_SIGNER("unknown");

  private final String type;

  SignerType(String type) {
    this.type = type;
  }

  private String getType() {
    return type;
  }

  public static SignerType fromString(String typeString) {
    if (FILE_BASED_SIGNER.getType().equals(typeString)) {
      return FILE_BASED_SIGNER;
    } else {
      return UNKNOWN_TYPE_SIGNER;
    }
  }
};

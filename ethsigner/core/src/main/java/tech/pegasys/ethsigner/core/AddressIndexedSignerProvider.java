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
package tech.pegasys.ethsigner.core;

import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.signers.secp256k1.api.SignerProvider;

import java.security.interfaces.ECPublicKey;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

public class AddressIndexedSignerProvider {
  private final SignerProvider signerProvider;

  public AddressIndexedSignerProvider(final SignerProvider signerProvider) {
    this.signerProvider = signerProvider;
  }

  public static AddressIndexedSignerProvider create(final SignerProvider signerProvider) {
    return new AddressIndexedSignerProvider(signerProvider);
  }

  /* Gets a signer from its address, NOTE address MUST have 0x hex prefix */
  public Optional<Signer> getSigner(final String address) {
    return signerProvider.getSigner(new Eth1AddressSignerIdentifier(address));
  }

  @VisibleForTesting
  public Set<String> availableAddresses() {
    return availablePublicKeys().stream()
        .map(Eth1AddressSignerIdentifier::fromPublicKey)
        .map(signerIdentifier -> "0x" + signerIdentifier.toStringIdentifier())
        .collect(Collectors.toSet());
  }

  public Set<ECPublicKey> availablePublicKeys() {
    return signerProvider.availablePublicKeys(Eth1AddressSignerIdentifier::fromPublicKey);
  }
}

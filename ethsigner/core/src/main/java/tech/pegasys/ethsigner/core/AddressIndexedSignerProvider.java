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

import tech.pegasys.signers.secp256k1.api.PublicKey;
import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.signers.secp256k1.api.SignerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Keys;

public class AddressIndexedSignerProvider {

  // Provides a signer based on its Public Key
  private final SignerProvider signerProvider;

  // String is an 0x prefixed hex string of the ethereum address corresponding to the public
  // key.
  private final Map<String, PublicKey> addressToPublicKeyMap;

  public AddressIndexedSignerProvider(
      final SignerProvider signerProvider, final Map<String, PublicKey> addressToPublicKeyMap) {
    this.signerProvider = signerProvider;
    this.addressToPublicKeyMap = addressToPublicKeyMap;
  }

  public static AddressIndexedSignerProvider create(final SignerProvider signerProvider) {
    final Map<String, PublicKey> addrToPubKeyMap = new HashMap<>();

    signerProvider
        .availablePublicKeys()
        .forEach(
            pubKey -> {
              final String address =
                  "0x" + Keys.getAddress(Bytes.wrap(pubKey.getValue()).toHexString()).toLowerCase();
              addrToPubKeyMap.put(address, pubKey);
            });

    return new AddressIndexedSignerProvider(signerProvider, addrToPubKeyMap);
  }

  /* Gets a signer from its address, NOTE address MUST have 0x hex prefix */
  public Optional<Signer> getSigner(final String address) {
    final PublicKey publicKey = addressToPublicKeyMap.get(address.toLowerCase());
    if (publicKey == null) {
      return Optional.empty();
    }
    return signerProvider.getSigner(publicKey);
  }

  public Set<String> availableAddresses() {
    return addressToPublicKeyMap.keySet();
  }

  public Set<PublicKey> availablePublicKeys() {
    return signerProvider.availablePublicKeys();
  }
}

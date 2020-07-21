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
package tech.pegasys.ethsigner.tests.signing;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.ENCLAVE_ERROR;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.GET_PRIVATE_TRANSACTION_NONCE_ERROR;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_LIMIT;
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.PrivateTransaction.RESTRICTED;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.dsl.PrivateTransaction;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;
import tech.pegasys.ethsigner.tests.signing.contract.generated.SimpleStorage;

import java.math.BigInteger;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class PrivateTransactionAcceptanceTest extends AcceptanceTestBase {
  private static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";

  @Test
  public void deployContract() {
    final PrivateTransaction contract =
        PrivateTransaction.createContractTransaction(
            richBenefactor().address(),
            BigInteger.ZERO,
            GAS_PRICE,
            GAS_LIMIT,
            BigInteger.ZERO,
            SimpleStorage.BINARY,
            enclavePublicKey(),
            singletonList(enclavePublicKey()),
            null,
            RESTRICTED);

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner().privateContracts().submitExceptional(contract);
    // We expect this to fail with enclave error as we don't have orion running. If rlp decode fails
    // then we would get a different error
    assertThat(signerResponse.jsonRpc().getError()).isEqualTo(ENCLAVE_ERROR);
  }

  @Test
  public void valueTransferWithNonZeroValue() {
    final PrivateTransaction transaction =
        PrivateTransaction.createEtherTransaction(
            richBenefactor().address(),
            Optional.of(BigInteger.ZERO),
            GAS_PRICE,
            GAS_LIMIT,
            RECIPIENT,
            BigInteger.ONE,
            enclavePublicKey(),
            singletonList(enclavePublicKey()),
            null,
            RESTRICTED);

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner().privateContracts().submitExceptional(transaction);
    assertThat(signerResponse.jsonRpc().getError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void valueTransferWithoutNonce() {
    final PrivateTransaction transaction =
        PrivateTransaction.createEtherTransaction(
            richBenefactor().address(),
            Optional.empty(),
            GAS_PRICE,
            GAS_LIMIT,
            RECIPIENT,
            BigInteger.ZERO,
            enclavePublicKey(),
            singletonList(enclavePublicKey()),
            null,
            RESTRICTED);

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner().privateContracts().submitExceptional(transaction);
    assertThat(signerResponse.jsonRpc().getError()).isEqualTo(GET_PRIVATE_TRANSACTION_NONCE_ERROR);
  }
}

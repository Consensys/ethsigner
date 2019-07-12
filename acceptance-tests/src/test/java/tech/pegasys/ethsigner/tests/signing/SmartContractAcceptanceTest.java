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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_LIMIT;
import static tech.pegasys.ethsigner.tests.dsl.Contracts.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.utils.Hex.hex;

import tech.pegasys.ethsigner.tests.AcceptanceTestBase;
import tech.pegasys.ethsigner.tests.signing.contract.generated.SimpleStorage;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.request.Transaction;

public class SmartContractAcceptanceTest extends AcceptanceTestBase {

  private static final String SIMPLE_STORAGE_BINARY = SimpleStorage.BINARY;
  private static final String SIMPLE_STORAGE_GET = "0x6d4ce63c";
  private static final String SIMPLE_STORAGE_SET_7 =
      "0x60fe47b10000000000000000000000000000000000000000000000000000000000000007";

  @Test
  public void deployContract() {
    final Transaction contract =
        Transaction.createContractTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            BigInteger.ZERO,
            SIMPLE_STORAGE_BINARY);

    final String hash = ethSigner().publicContracts().submit(contract);
    ethNode().publicContracts().awaitBlockContaining(hash);

    final String address = ethNode().publicContracts().address(hash);
    final String code = ethNode().publicContracts().code(address);
    assertThat(code)
        .isEqualTo(
            "0x60806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166360fe47b18114604d5780636d4ce63c146075575b600080fd5b348015605857600080fd5b50607360048036036020811015606d57600080fd5b50356099565b005b348015608057600080fd5b506087609e565b60408051918252519081900360200190f35b600055565b6000549056fea165627a7a72305820cb1d0935d14b589300b12fcd0ab849a7e9019c81da24d6daa4f6b2f003d1b0180029");
  }

  @Test
  public void invokeContract() {
    final Transaction contract =
        Transaction.createContractTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            BigInteger.ZERO,
            SIMPLE_STORAGE_BINARY);

    final String hash = ethSigner().publicContracts().submit(contract);
    ethNode().publicContracts().awaitBlockContaining(hash);

    final String contractAddress = ethNode().publicContracts().address(hash);
    final Transaction valueBeforeChange =
        Transaction.createEthCallTransaction(
            richBenefactor().address(), contractAddress, SIMPLE_STORAGE_GET);
    final BigInteger startingValue = hex(ethSigner().publicContracts().call(valueBeforeChange));
    final Transaction changeValue =
        Transaction.createFunctionCallTransaction(
            richBenefactor().address(),
            richBenefactor().nextNonceAndIncrement(),
            GAS_PRICE,
            GAS_LIMIT,
            contractAddress,
            SIMPLE_STORAGE_SET_7);

    final String valueUpdate = ethSigner().publicContracts().submit(changeValue);
    ethNode().publicContracts().awaitBlockContaining(valueUpdate);

    final Transaction valueAfterChange =
        Transaction.createEthCallTransaction(
            richBenefactor().address(), contractAddress, SIMPLE_STORAGE_GET);
    final BigInteger endValue = hex(ethSigner().publicContracts().call(valueAfterChange));
    assertThat(endValue).isEqualTo(startingValue.add(BigInteger.valueOf(7)));
  }
}

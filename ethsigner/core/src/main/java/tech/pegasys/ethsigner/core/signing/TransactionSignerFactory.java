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
package tech.pegasys.ethsigner.core.signing;

import tech.pegasys.ethsigner.core.signing.filebased.FileBasedTransactionSigner;
import tech.pegasys.ethsigner.core.signing.hashicorp.HashicorpTransactionSigner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransactionSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  public static TransactionSigner create(final TransactionSignerConfig config) {
    if (config.name().equals(HashicorpTransactionSigner.class.getName())) {
      return new HashicorpTransactionSigner(config);
    } else if (config.name().equals(FileBasedTransactionSigner.class.getName())) {
      return new FileBasedTransactionSigner(config);
    } else {
      final String message = "Don't know about TransactionSigner with name: " + config.name();
      LOG.error(message);
      throw new TransactionSignerInitializationException(message);
    }
  }
}

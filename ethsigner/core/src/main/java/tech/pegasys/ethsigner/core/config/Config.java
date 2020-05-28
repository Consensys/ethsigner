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
package tech.pegasys.ethsigner.core.config;

import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.ethsigner.core.signing.ChainIdProvider;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.apache.logging.log4j.Level;

public interface Config {

  Level getLogLevel();

  String getDownstreamHttpHost();

  Integer getDownstreamHttpPort();

  String getDownstreamHttpPath();

  Duration getDownstreamHttpRequestTimeout();

  String getHttpListenHost();

  Integer getHttpListenPort();

  ChainIdProvider getChainId();

  Path getDataPath();

  Optional<TlsOptions> getTlsOptions();

  Optional<ClientTlsOptions> getClientTlsOptions();
}

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
package tech.pegasys.ethsigner.signer.hashicorp;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Path;

public class HashicorpConfig {
  private final String signingKeyPath;
  private final String host;
  private final Integer port;
  private final Path authFilePath;
  private final Long timeout;

  public HashicorpConfig(
      final String signingKeyPath,
      final String host,
      final Integer port,
      final Path authFilePath,
      final Long timeout) {
    this.signingKeyPath = signingKeyPath;
    this.host = host;
    this.port = port;
    this.authFilePath = authFilePath;
    this.timeout = timeout;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public Path getAuthFilePath() {
    return authFilePath;
  }

  public Long getTimeout() {
    return timeout;
  }

  public String getSigningKeyPath() {
    return signingKeyPath;
  }

  public static class HashicorpConfigBuilder {

    private String signingKeyPath;
    private String host;
    private Integer port;
    private Path authFilePath;
    private Long timeout;

    public HashicorpConfigBuilder withSigningKeyPath(final String signingKeyPath) {
      this.signingKeyPath = signingKeyPath;
      return this;
    }

    public HashicorpConfigBuilder withHost(final String host) {
      this.host = host;
      return this;
    }

    public HashicorpConfigBuilder withPort(final Integer port) {
      this.port = port;
      return this;
    }

    public HashicorpConfigBuilder withAuthFilePath(final Path authFilePath) {
      this.authFilePath = authFilePath;
      return this;
    }

    public HashicorpConfigBuilder withTimeout(final Long timeout) {
      this.timeout = timeout;
      return this;
    }

    public HashicorpConfig build() {
      checkNotNull(signingKeyPath, "Signing Key Path was not set.");
      checkNotNull(host, "Host was not set.");
      checkNotNull(port, "Port was not set.");
      checkNotNull(authFilePath, "Auth File Path was not set.");
      checkNotNull(timeout, "Timeout was not set.");

      return new HashicorpConfig(signingKeyPath, host, port, authFilePath, timeout);
    }
  }
}

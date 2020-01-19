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
package tech.pegasys.ethsigner.tests.dsl.hashicorp;

import static java.nio.file.Files.createTempDirectory;

import tech.pegasys.ethsigner.tests.tls.SelfSignedPfxStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import io.vertx.core.net.PfxOptions;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * To run Hashicorp Vault docker in TLS mode, we create self-signed certificates in a temporary
 * directory which can be mounted to docker.
 *
 * <p>The directory returned by Java system property java.io.tmpdir may not be mountable by docker
 * in Mac or Windows environment. Therefore this class attempts to use Java system property
 * 'user.home' and create a mountable directory in it. Directory returned by Java system property
 * `user.home` is typically enabled in docker preference in MacOSx and Windows. See "docker ->
 * Preferences -> File Sharing" to manage mount points.
 */
public class HashicorpVaultCertificate {
  private static final Path MOUNTABLE_PARENT_DIR =
      Path.of(System.getProperty("user.home", System.getProperty("java.io.tmpdir", "/tmp")));
  private static final Logger LOG = LogManager.getLogger();
  private static final String TEMP_DIR_PREFIX = ".ethsigner-vault-dsl";

  private SelfSignedPfxStore selfSignedPfxStore;
  private Path certificateDirectory;
  private Path tlsCertificate;
  private Path tlsPrivateKey;

  private HashicorpVaultCertificate() {}

  public static HashicorpVaultCertificate create() {
    try {
      final HashicorpVaultCertificate hashicorpVaultCertificate = new HashicorpVaultCertificate();
      hashicorpVaultCertificate.createCertificateDirectory();
      hashicorpVaultCertificate.createSelfSignedCertificates();
      hashicorpVaultCertificate.copyKeyCertificatesInCertificateDirectory();
      return hashicorpVaultCertificate;
    } catch (final Exception e) {
      LOG.error("Unable to initialise HashicorpVaultCertificates", e);
      throw new RuntimeException("Unable to initialise HashicorpVaultCertificates", e);
    }
  }

  private void createCertificateDirectory() throws IOException {
    // allows docker process to have access
    final Set<PosixFilePermission> posixPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
    final FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(posixPermissions);
    certificateDirectory = createTempDirectory(MOUNTABLE_PARENT_DIR, TEMP_DIR_PREFIX, permissions);
    FileUtils.forceDeleteOnExit(certificateDirectory.toFile());
  }

  private void createSelfSignedCertificates() throws Exception {
    final Path tempDirectory = createTempDirectory("hashicorp_tls");
    FileUtils.forceDeleteOnExit(tempDirectory.toFile());
    selfSignedPfxStore = SelfSignedPfxStore.create(tempDirectory);
  }

  private void copyKeyCertificatesInCertificateDirectory() throws IOException {
    final Path sourceCertificatePath = selfSignedPfxStore.getCertificatePath();
    tlsCertificate = certificateDirectory.resolve(sourceCertificatePath.getFileName());
    Files.copy(sourceCertificatePath, tlsCertificate);

    final Path sourcePrivateKeyPath = selfSignedPfxStore.getPrivateKeyPath();
    tlsPrivateKey = certificateDirectory.resolve(sourcePrivateKeyPath.getFileName());
    Files.copy(sourcePrivateKeyPath, tlsPrivateKey);
  }

  public Path getCertificateDirectory() {
    return certificateDirectory;
  }

  public Path getTlsCertificate() {
    return tlsCertificate;
  }

  public Path getTlsPrivateKey() {
    return tlsPrivateKey;
  }

  public PfxOptions getPfxTrustOptions() {
    return new PfxOptions()
        .setPath(selfSignedPfxStore.getTrustStoreFile().toString())
        .setPassword(new String(selfSignedPfxStore.getPassword()));
  }

  public Path getPfxPasswordFile() {
    return selfSignedPfxStore.getPasswordFile();
  }
}

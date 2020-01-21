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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * To run Hashicorp Vault docker in TLS mode, we copy self-signed certificates in a temporary
 * directory which can be mounted to docker.
 *
 * <p>The directory returned by Java system property java.io.tmpdir may not be mountable by docker
 * in Mac or Windows environment. Therefore this class attempts to use Java system property
 * 'user.home' and create a mountable directory in it. Directory returned by Java system property
 * `user.home` is typically enabled in docker preference in MacOSx and Windows. See "docker ->
 * Preferences -> File Sharing" to manage mount points.
 */
public class HashicorpVaultDockerCertificate {
  private static final Path MOUNTABLE_PARENT_DIR =
      Path.of(System.getProperty("user.home", System.getProperty("java.io.tmpdir", "/tmp")));
  private static final Logger LOG = LogManager.getLogger();
  private static final String TEMP_DIR_PREFIX = ".ethsigner-vault-dsl";

  private SelfSignedCertificate selfSignedCertificate;
  private Path certificateDirectory;
  private Path tlsCertificate;
  private Path tlsPrivateKey;

  private HashicorpVaultDockerCertificate() {}

  public static HashicorpVaultDockerCertificate create() {
    try {
      final HashicorpVaultDockerCertificate hashicorpVaultDockerCertificate =
          new HashicorpVaultDockerCertificate();
      hashicorpVaultDockerCertificate.createCertificateDirectory();
      hashicorpVaultDockerCertificate.createSelfSignedCertificates();
      hashicorpVaultDockerCertificate.copyKeyCertificatesInCertificateDirectory();
      return hashicorpVaultDockerCertificate;
    } catch (final Exception e) {
      LOG.error("Unable to initialize HashicorpVaultCertificates", e);
      throw new RuntimeException("Unable to initialize HashicorpVaultCertificates", e);
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
    selfSignedCertificate = SelfSignedCertificate.generate();
  }

  private void copyKeyCertificatesInCertificateDirectory() throws IOException {
    final Set<PosixFilePermission> posixPermissions = PosixFilePermissions.fromString("rw-r--r--");

    final Path sourceCertificatePath = selfSignedCertificate.certificatePath();
    tlsCertificate = certificateDirectory.resolve(sourceCertificatePath.getFileName());
    Files.copy(sourceCertificatePath, tlsCertificate);
    Files.setPosixFilePermissions(tlsCertificate, posixPermissions);

    final Path sourcePrivateKeyPath = selfSignedCertificate.privateKeyPath();
    tlsPrivateKey = certificateDirectory.resolve(sourcePrivateKeyPath.getFileName());
    Files.copy(sourcePrivateKeyPath, tlsPrivateKey);
    Files.setPosixFilePermissions(tlsPrivateKey, posixPermissions);
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
}

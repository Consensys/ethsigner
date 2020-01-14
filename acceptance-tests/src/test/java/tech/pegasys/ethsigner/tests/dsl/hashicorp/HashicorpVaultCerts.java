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

import static java.nio.file.Files.createFile;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.writeString;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hashicorp Vault docker is created in server mode with TLS enabled. This requires passing self
 * signed certificates as defined by this class to the vault docker container as mount directory.
 * This class uses Java system property 'user.home' to create a temporary mount directory and
 * generate tls certs in it as it is typically enabled in docker preference in MacOSx and Windows.
 * See "docker -> Preferences -> File Sharing" to manage mount points.
 */
public class HashicorpVaultCerts {
  private static final Path MOUNT_PARENT_DIR =
      Path.of(System.getProperty("user.home", System.getProperty("java.io.tmpdir", "/tmp")));
  private static final Logger LOG = LogManager.getLogger();
  private static final String TEMP_PREFIX = ".vault-at";

  // the certs are generated using command:
  /*
  cat <<EOF > ./req.conf
  [req]
  distinguished_name = req_distinguished_name
  x509_extensions = v3_req
  prompt = no
  [req_distinguished_name]
  C = AU
  ST = QLD
  L = Brisbane
  O = PegaSys
  OU = Prod Dev
  CN = localhost
  [v3_req]
  keyUsage = keyEncipherment, dataEncipherment
  extendedKeyUsage = serverAuth
  subjectAltName = @alt_names
  [alt_names]
  DNS.1 = localhost
  IP.1 = 127.0.0.1
  EOF

  openssl req -x509 -nodes -days 36500 -newkey rsa:2048 -keyout vault.key -out vault.crt \
   -config req.conf -extensions 'v3_req'
   */
  private static final String VAULT_CERTIFICATE =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIIDkzCCAnugAwIBAgIJALx0otyWA546MA0GCSqGSIb3DQEBCwUAMGcxCzAJBgNV\n"
          + "BAYTAkFVMQwwCgYDVQQIDANRTEQxETAPBgNVBAcMCEJyaXNiYW5lMRAwDgYDVQQK\n"
          + "DAdQZWdhU3lzMREwDwYDVQQLDAhQcm9kIERldjESMBAGA1UEAwwJbG9jYWxob3N0\n"
          + "MCAXDTIwMDExNDAwMzYxOFoYDzIxMTkxMjIxMDAzNjE4WjBnMQswCQYDVQQGEwJB\n"
          + "VTEMMAoGA1UECAwDUUxEMREwDwYDVQQHDAhCcmlzYmFuZTEQMA4GA1UECgwHUGVn\n"
          + "YVN5czERMA8GA1UECwwIUHJvZCBEZXYxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASIw\n"
          + "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKJPoOndx/jAlclEDh8okP6k1AR9\n"
          + "7YTJHzHXVxq7Sulin8ybuXNrh7wRe3uxDM2C3wSPQ6FU3IxvG+qLlWkILm6mUutH\n"
          + "0D/4Df6rW1ylGIxn6K6d/LMd39XHLViMStJG6JgHxNxZw3MwfV/DK0f3703Fri6i\n"
          + "wBt/B1XRDdV4m41vzJDXFQuyJGjs+CXnCjVSRMf4rCJQqp0kJvJThoETBXoYKLea\n"
          + "vOls8wa4jF6nYVJLDeyEhl5nZaPB/OPIl2qf+YYk9CyarlrfFXgFCdX6d214mgzR\n"
          + "eiseDNyk887Ha9B0OFgDZxfDfYVt567Q8O+6IB57Q+3WjTLvDv88rfDXKTECAwEA\n"
          + "AaNAMD4wCwYDVR0PBAQDAgQwMBMGA1UdJQQMMAoGCCsGAQUFBwMBMBoGA1UdEQQT\n"
          + "MBGCCWxvY2FsaG9zdIcEfwAAATANBgkqhkiG9w0BAQsFAAOCAQEAQ6mVVMuQPvHR\n"
          + "laJM1PaiKGG1C+zpaoBPpswEVe8xyJeNFvhhEyoLP9ILJ18dFyNaVYqR2ph6XXUs\n"
          + "3BcjL1bcR3sNKhYooyIIII3T2gY4sL4TipRNaBQP6HN2f88aTN+3DRgfXZe7qLwU\n"
          + "bToe2ZhjR8n6OpT59zVS77beUFv2k/sR2exYqEm0UB3MWCn0vstYyqUAoKxPeEee\n"
          + "/+hB6Qsm5ivrlLSiTjQlCNv9MKxRXzFdCsKTE5OBQ3BpVGpeBkV2XDbJU6Kvzr8d\n"
          + "2FcD5CyJ6j088vNIVvYMxpIE3YRyi8N+Xs5bB3wXVDQ+u/PzuLvs9+5IUxmDurm2\n"
          + "YF29WQgGqg==\n"
          + "-----END CERTIFICATE-----";
  private static final String VAULT_KEY =
      "-----BEGIN PRIVATE KEY-----\n"
          + "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCiT6Dp3cf4wJXJ\n"
          + "RA4fKJD+pNQEfe2EyR8x11cau0rpYp/Mm7lza4e8EXt7sQzNgt8Ej0OhVNyMbxvq\n"
          + "i5VpCC5uplLrR9A/+A3+q1tcpRiMZ+iunfyzHd/Vxy1YjErSRuiYB8TcWcNzMH1f\n"
          + "wytH9+9Nxa4uosAbfwdV0Q3VeJuNb8yQ1xULsiRo7Pgl5wo1UkTH+KwiUKqdJCby\n"
          + "U4aBEwV6GCi3mrzpbPMGuIxep2FSSw3shIZeZ2WjwfzjyJdqn/mGJPQsmq5a3xV4\n"
          + "BQnV+ndteJoM0XorHgzcpPPOx2vQdDhYA2cXw32Fbeeu0PDvuiAee0Pt1o0y7w7/\n"
          + "PK3w1ykxAgMBAAECggEAS06Uw01CRdjc5lfsEWrZ8zv4nujqdex2y8I0yNNTS3uV\n"
          + "1vH9ll6yyIB4AYjA/u0UmmH6J/Veqs22bxk6RlQkbvQ+jqlwJu3pWFqa9h4niKWB\n"
          + "YNsubtCSlPZnxKi64ltB+Z/JJ8+CDoTV9sNv5mFTp4rraPncFiXW+msXdw8RZQpX\n"
          + "suAFkv8nDetKUZlyHQxa+QHte6SKw73lQ0eEogZmeDOqhOuayb11iLVW8r1NtbB6\n"
          + "oXafoWbwY/6vUVI4IOqd1NbT4YuRKCRqZhL3E1J3jq5F6iYFY8hsEYMBdTBLRaC1\n"
          + "kypP7rAgjizQvOBJSjuews8XOYHMK4FIrewpcD0OLQKBgQDXvLmVo1ffEVyOvqU+\n"
          + "ACWZkysg2Wgz77U7KSqjSoYDB0tTAEs+WmQ/jeW4sb48F93A4htILyMM01m2AiN+\n"
          + "z4Y3w+AHgH18EG93HlZFXwXbC2bYqB4ZvaK4jHbmQA2FeKkVQwNbJo3gc5Iz4B68\n"
          + "UKKpsujfy3ZVHYZLWEmBl1R3IwKBgQDAmlzJ7vfXswbGKFwcHicWM6RUEUQTszHf\n"
          + "vrakEtYvMOLAi6O4vYWyhOoZpWHFTQgXkkSpO8O62MihidQ7H4KaKxk2/j0dpkii\n"
          + "LUkfSaOnV/FNnDM1PUAPXqBgWXXlDAwBLeelsSxTQcBdwhv9hbvpxnlpTpfFZ4I0\n"
          + "jPyXWETNmwKBgESxr1ZagKxi1toNEoi/ezl/hwgasfd3qHmCDjWYkTt9oxp2yX69\n"
          + "QZaLFE7PKgfwgYfdd1pwx/RZiANQgwTqB47SLA+3dj1+7j87xbSpITAyXTk3rv37\n"
          + "SEkDUQjX9HY/iXdfsz3e9OY3ZqJmBuNnuaPXeBdqre9ES3LKVf2Rti+JAoGAIixB\n"
          + "QmLv/ol5LjeDAEopTTskcPYPSh+FPdmstjfRYNYfpWIhTmnyCtaiYqKBUmx0fxoo\n"
          + "rI46EDDeiCrYSRUyKwBynNtHJLeYM44tZyu9qbdkzQxl2ZBgiVmUwwOcc2NLzfnD\n"
          + "HBbvcmY8J2aFaNoNmVDpwdF8BD51T7WkK4YxzvkCgYAdkIlyIi1wxAD6ou+V3yYL\n"
          + "IWL07iv+tq8QO39l/YgO7zC4YZdfxoOl4wmKGLx7LzSnFtNOp9nYKIE/dUQoUt0m\n"
          + "ahwqcgCf6wa3eJquykYN6L0Aq8VJUqmsSWPjT9YTNEWLjIiJxn8lNPVSXbcRukUQ\n"
          + "aqFZiNpVTo8aTLGifLeBIw==\n"
          + "-----END PRIVATE KEY-----";

  private static final Set<PosixFilePermission> DIR_POSIX_PERMISSIONS =
      EnumSet.of(
          OWNER_READ,
          OWNER_WRITE,
          OWNER_EXECUTE,
          GROUP_READ,
          GROUP_EXECUTE,
          OTHERS_READ,
          OTHERS_EXECUTE);
  private static final Set<PosixFilePermission> FILES_POSIX_PERMISSIONS =
      EnumSet.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ);

  private final Path certificateDirectory;
  private final Path tlsCertificate;
  private final Path tlsPrivateKey;

  public HashicorpVaultCerts() {
    try {
      final boolean isPosix =
          FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
      final List<FileAttribute<?>> dirAttr =
          isPosix ? singletonList(asFileAttribute(DIR_POSIX_PERMISSIONS)) : emptyList();
      final List<FileAttribute<?>> fileAttr =
          isPosix ? singletonList(asFileAttribute(FILES_POSIX_PERMISSIONS)) : emptyList();

      certificateDirectory =
          createTempDirectory(
              MOUNT_PARENT_DIR, TEMP_PREFIX, dirAttr.toArray(FileAttribute<?>[]::new));
      tlsCertificate =
          createFile(
              certificateDirectory.resolve("vault.crt"), fileAttr.toArray(FileAttribute<?>[]::new));
      tlsPrivateKey =
          createFile(
              certificateDirectory.resolve("vault.key"), fileAttr.toArray(FileAttribute<?>[]::new));

      LOG.debug("Temporary cert directory: {}", certificateDirectory.toString());

      writeString(tlsCertificate, VAULT_CERTIFICATE);
      writeString(tlsPrivateKey, VAULT_KEY);

      Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
    } catch (IOException ioe) {
      throw new RuntimeException("Unable to create temporary certificates", ioe);
    }
  }

  private void cleanup() {
    try {
      FileUtils.deleteDirectory(certificateDirectory.toFile());
    } catch (IOException e) {
      LOG.warn("Deletion failed for tls certificates", e);
    }
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

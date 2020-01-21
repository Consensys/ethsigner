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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.Period;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * Utility class to generate temporary self-signed certificates with SAN extension for multiple DNS
 * and IP addresses in PEM formats for testing purposes using BouncyCastle APIs.
 *
 * <p>Note: DO NOT USE IN PRODUCTION. The generated stores and files are marked to be deleted on JVM
 * exit.
 */
public final class SelfSignedCertificate {
  private static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();
  private static final boolean IS_CA = true;
  private static final String distinguishedName = "CN=localhost";
  private static final List<String> sanHostNames = List.of("localhost");
  private static final List<String> sanIpAddresses = List.of("127.0.0.1");
  private final Path certificatePath;
  private final Path privateKeyPath;

  private SelfSignedCertificate(final Path certificatePath, final Path privateKeyPath) {
    this.certificatePath = certificatePath;
    this.privateKeyPath = privateKeyPath;
  }

  public static SelfSignedCertificate generate() {
    try {
      final KeyPair keyPair = generateKeyPair();
      return new SelfSignedCertificate(
          createPemCertificateFile(generateSelfSignedCertificate(keyPair)),
          createPemPrivateKeyFile(keyPair.getPrivate()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static X509Certificate generateSelfSignedCertificate(final KeyPair keyPair)
      throws CertIOException, GeneralSecurityException, OperatorCreationException {
    final X500Name issuer = new X500Name(distinguishedName);
    final X500Name subject = new X500Name(distinguishedName);
    final BigInteger serialNumber = new BigInteger(String.valueOf(Instant.now().toEpochMilli()));
    final X509v3CertificateBuilder v3CertificateBuilder =
        new JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            Date.from(Instant.now()),
            Date.from(Instant.now().plus(Period.ofDays(90))),
            subject,
            keyPair.getPublic());

    // extensions
    v3CertificateBuilder.addExtension(
        Extension.basicConstraints, true, new BasicConstraints(IS_CA));
    v3CertificateBuilder.addExtension(
        Extension.subjectAlternativeName, false, getSubjectAlternativeNames());

    final ContentSigner contentSigner =
        new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());

    return new JcaX509CertificateConverter()
        .setProvider(BOUNCY_CASTLE_PROVIDER)
        .getCertificate(v3CertificateBuilder.build(contentSigner));
  }

  private static KeyPair generateKeyPair() throws GeneralSecurityException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048, new SecureRandom());
    return keyPairGenerator.generateKeyPair();
  }

  private static GeneralNames getSubjectAlternativeNames() {
    final List<GeneralName> hostGeneralNames =
        sanHostNames.stream()
            .map(hostName -> new GeneralName(GeneralName.dNSName, hostName))
            .collect(Collectors.toList());
    final List<GeneralName> ipGeneralNames =
        sanIpAddresses.stream()
            .map(ipAddress -> new GeneralName(GeneralName.iPAddress, ipAddress))
            .collect(Collectors.toList());
    final GeneralName[] generalNames =
        Stream.of(hostGeneralNames, ipGeneralNames)
            .flatMap(Collection::stream)
            .toArray(GeneralName[]::new);

    return new GeneralNames(generalNames);
  }

  private static Path createPemCertificateFile(final Certificate certificate)
      throws IOException, CertificateEncodingException {
    final Path certificatePath = Files.createTempFile("test", ".crt");
    certificatePath.toFile().deleteOnExit();

    try (final BufferedWriter writer = Files.newBufferedWriter(certificatePath, UTF_8);
        final PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
    }

    return certificatePath;
  }

  private static Path createPemPrivateKeyFile(final PrivateKey privateKey) throws IOException {
    final Path privateKeyPath = Files.createTempFile("test", ".key");
    privateKeyPath.toFile().deleteOnExit();

    try (final BufferedWriter writer = Files.newBufferedWriter(privateKeyPath, UTF_8);
        final PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
    }
    return privateKeyPath;
  }

  public Path certificatePath() {
    return certificatePath;
  }

  public Path privateKeyPath() {
    return privateKeyPath;
  }
}

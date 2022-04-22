/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.KeyManagerBuilderException;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Responsible for building a X509KeyManager.
 *
 * Explicit key algorithm SunX509 is used to ensure we actually create a X509KeyManager.
 * This is similar to the default algorithm that's returned if nothing else configured.
 *
 * Default values are found here:
 *    https://github.com/JetBrains/jdk8u_jdk/blob/master/src/share/classes/javax/net/ssl/KeyManagerFactory.java
 *
 * Registered classes for the algorithms can be found here:
 *    https://github.com/JetBrains/jdk8u_jdk/blob/master/src/share/classes/sun/security/ssl/SunJSSE.java
 */
@OwnedBy(PL)
public class X509KeyManagerBuilder {
  public static final String KEY_MANAGER_ALGORITHM_SUNX509 = "SunX509";
  public static final String KEY_ALGORITHM_RSA = "RSA";
  public static final String KEY_STORE_TYPE_JAVA = "jks";
  private static final String CERTIFICATE_TYPE_X509 = "X.509";
  private static final String PEM_PRIVATE_KEY_START = "-----BEGIN PRIVATE KEY-----";
  private static final String PEM_PRIVATE_KEY_END = "-----END PRIVATE KEY-----";
  private static final String CLIENT_CERTIFICATE_KEY_ENTRY_NAME = "client";
  private static final int KEY_STORE_PASSWORD_LENGTH = 12;

  private final char[] keyStorePassword;
  private final KeyStore keystore;

  public X509KeyManagerBuilder() throws KeyManagerBuilderException {
    this.keyStorePassword = RandomStringUtils.randomAlphanumeric(KEY_STORE_PASSWORD_LENGTH).toCharArray();

    try {
      this.keystore = KeyStore.getInstance(KEY_STORE_TYPE_JAVA);
      this.keystore.load(null, null);
    } catch (Exception ex) {
      throw new KeyManagerBuilderException(String.format("Failed to create empty key store: %s", ex.getMessage()), ex);
    }
  }

  /**
   * Configures the KeyManager with a client certificate.
   *
   * @param certPath The path to the cert file (PEM format) - can contain cert chain.
   * @param keyPath The path to the key file (PEM format with PKCS8 encoded RSA key).
   * @return the builder.
   * @throws KeyManagerBuilderException in case of any issues loading the certificate into the key store.
   */
  public X509KeyManagerBuilder withClientCertificateFromFile(String certPath, String keyPath)
      throws KeyManagerBuilderException {
    PrivateKey privateKey = this.loadPkcs8EncodedPrivateKeyFromPem(keyPath);
    Certificate[] certificates = this.loadCertificatesFromPem(certPath);

    try {
      this.keystore.setKeyEntry(CLIENT_CERTIFICATE_KEY_ENTRY_NAME, privateKey, keyStorePassword, certificates);
    } catch (Exception ex) {
      throw new KeyManagerBuilderException(
          String.format("Failed to generate keystore from certificate: %s", ex.getMessage()), ex);
    }

    return this;
  }

  public X509KeyManager build() throws KeyManagerBuilderException {
    try {
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KEY_MANAGER_ALGORITHM_SUNX509);
      keyManagerFactory.init(this.keystore, this.keyStorePassword);

      // We only expect one KeyManager from the SunX509 factory
      return (X509KeyManager) keyManagerFactory.getKeyManagers()[0];
    } catch (Exception ex) {
      throw new KeyManagerBuilderException(String.format("Failed to create key manager: %s", ex.getMessage()), ex);
    }
  }

  private Certificate[] loadCertificatesFromPem(String filePath) throws KeyManagerBuilderException {
    try (InputStream certificateChainAsInputStream =
             Files.newInputStream(Paths.get(filePath), StandardOpenOption.READ)) {
      CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE_X509);
      return certificateFactory.generateCertificates(certificateChainAsInputStream).toArray(new Certificate[0]);
    } catch (Exception ex) {
      throw new KeyManagerBuilderException(
          String.format("Failed to load certificate(s) from '%s': %s", filePath, ex.getMessage()), ex);
    }
  }

  private PrivateKey loadPkcs8EncodedPrivateKeyFromPem(String filePath) throws KeyManagerBuilderException {
    return this.loadPkcs8EncodedPrivateKeyFromPem(filePath, KEY_ALGORITHM_RSA);
  }

  private PrivateKey loadPkcs8EncodedPrivateKeyFromPem(String filePath, String keyAlgorithm)
      throws KeyManagerBuilderException {
    String fileContent = null;
    try {
      byte[] rawFileContent = Files.readAllBytes(Paths.get(filePath));
      fileContent = new String(rawFileContent, Charset.defaultCharset());
    } catch (Exception ex) {
      throw new KeyManagerBuilderException(
          String.format("Failed to read file '%s': %s", filePath, ex.getMessage()), ex);
    }

    // prepare string - some sanitation to avoid to many complications.
    fileContent = fileContent.trim().replace("\n", "").replace("\r", "");

    // ensure file is in PEM format and has only one key
    int lastKeyStartTag = fileContent.lastIndexOf(PEM_PRIVATE_KEY_START);
    int firstKeyEndTag = fileContent.indexOf(PEM_PRIVATE_KEY_END);
    if (lastKeyStartTag != 0 || firstKeyEndTag == -1
        || firstKeyEndTag != fileContent.length() - PEM_PRIVATE_KEY_END.length()) {
      throw new KeyManagerBuilderException(String.format(
          "Invalid format for key (expected PEM format with DER encoded key in PKCS8 format) - Only one key per file is allowed and it has to start with %s and end with %s.",
          PEM_PRIVATE_KEY_START, PEM_PRIVATE_KEY_END));
    }

    // remove start and end tag
    String base64DerEncodedPrivateKey = fileContent.replace(PEM_PRIVATE_KEY_START, "").replace(PEM_PRIVATE_KEY_END, "");

    try {
      // decode to raw DER encoded key
      byte[] derEncodedPrivateKey = Base64.getDecoder().decode(base64DerEncodedPrivateKey);

      // generate private key
      KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(derEncodedPrivateKey);

      return keyFactory.generatePrivate(keySpec);
    } catch (Exception ex) {
      throw new KeyManagerBuilderException(
          String.format("Failed to generate key (expected DER encoded key in PKCS8 format): %s", ex.getMessage()), ex);
    }
  }
}

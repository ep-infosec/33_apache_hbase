/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.io.crypto.tls;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Base64.getMimeDecoder;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javax.crypto.Cipher.DECRYPT_MODE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * This file has been copied from the Apache ZooKeeper project.
 * @see <a href=
 *      "https://github.com/apache/zookeeper/blob/c74658d398cdc1d207aa296cb6e20de00faec03e/zookeeper-server/src/main/java/org/apache/zookeeper/util/PemReader.java">Base
 *      revision</a>
 */
final class PemReader {
  private static final Pattern CERT_PATTERN =
    Pattern.compile("-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
      "([a-z0-9+/=\\r\\n]+)" + // Base64 text
      "-+END\\s+.*CERTIFICATE[^-]*-+", // Footer
      CASE_INSENSITIVE);

  private static final Pattern PRIVATE_KEY_PATTERN =
    Pattern.compile("-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
      "([a-z0-9+/=\\r\\n]+)" + // Base64 text
      "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", // Footer
      CASE_INSENSITIVE);

  private static final Pattern PUBLIC_KEY_PATTERN =
    Pattern.compile("-+BEGIN\\s+.*PUBLIC\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
      "([a-z0-9+/=\\r\\n]+)" + // Base64 text
      "-+END\\s+.*PUBLIC\\s+KEY[^-]*-+", // Footer
      CASE_INSENSITIVE);

  private PemReader() {
  }

  public static KeyStore loadTrustStore(File certificateChainFile)
    throws IOException, GeneralSecurityException {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, null);

    List<X509Certificate> certificateChain = readCertificateChain(certificateChainFile);
    for (X509Certificate certificate : certificateChain) {
      X500Principal principal = certificate.getSubjectX500Principal();
      keyStore.setCertificateEntry(principal.getName("RFC2253"), certificate);
    }
    return keyStore;
  }

  public static KeyStore loadKeyStore(File certificateChainFile, File privateKeyFile,
    char[] keyPassword) throws IOException, GeneralSecurityException {
    PrivateKey key = loadPrivateKey(privateKeyFile, keyPassword);

    List<X509Certificate> certificateChain = readCertificateChain(certificateChainFile);
    if (certificateChain.isEmpty()) {
      throw new CertificateException(
        "Certificate file does not contain any certificates: " + certificateChainFile);
    }

    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, null);
    keyStore.setKeyEntry("key", key, keyPassword, certificateChain.toArray(new Certificate[0]));
    return keyStore;
  }

  public static List<X509Certificate> readCertificateChain(File certificateChainFile)
    throws IOException, GeneralSecurityException {
    String contents = new String(Files.readAllBytes(certificateChainFile.toPath()), US_ASCII);
    return readCertificateChain(contents);
  }

  public static List<X509Certificate> readCertificateChain(String certificateChain)
    throws CertificateException {
    Matcher matcher = CERT_PATTERN.matcher(certificateChain);
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    List<X509Certificate> certificates = new ArrayList<>();

    int start = 0;
    while (matcher.find(start)) {
      byte[] buffer = base64Decode(matcher.group(1));
      certificates.add(
        (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(buffer)));
      start = matcher.end();
    }

    return certificates;
  }

  public static PrivateKey loadPrivateKey(File privateKeyFile, char[] keyPassword)
    throws IOException, GeneralSecurityException {
    String privateKey = new String(Files.readAllBytes(privateKeyFile.toPath()), US_ASCII);
    return loadPrivateKey(privateKey, keyPassword);
  }

  public static PrivateKey loadPrivateKey(String privateKey, char[] keyPassword)
    throws IOException, GeneralSecurityException {
    Matcher matcher = PRIVATE_KEY_PATTERN.matcher(privateKey);
    if (!matcher.find()) {
      throw new KeyStoreException("did not find a private key");
    }
    byte[] encodedKey = base64Decode(matcher.group(1));

    PKCS8EncodedKeySpec encodedKeySpec;
    if (keyPassword != null && keyPassword.length > 0) {
      EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(encodedKey);
      SecretKeyFactory keyFactory =
        SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
      SecretKey secretKey = keyFactory.generateSecret(new PBEKeySpec(keyPassword));

      Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
      cipher.init(DECRYPT_MODE, secretKey, encryptedPrivateKeyInfo.getAlgParameters());

      encodedKeySpec = encryptedPrivateKeyInfo.getKeySpec(cipher);
    } else {
      encodedKeySpec = new PKCS8EncodedKeySpec(encodedKey);
    }

    // this code requires a key in PKCS8 format which is not the default openssl format
    // to convert to the PKCS8 format you use : openssl pkcs8 -topk8 ...
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(encodedKeySpec);
    } catch (InvalidKeySpecException ignore) {
      // ignore
    }

    try {
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      return keyFactory.generatePrivate(encodedKeySpec);
    } catch (InvalidKeySpecException ignore) {
      // ignore
    }

    KeyFactory keyFactory = KeyFactory.getInstance("DSA");
    return keyFactory.generatePrivate(encodedKeySpec);
  }

  public static PublicKey loadPublicKey(File publicKeyFile)
    throws IOException, GeneralSecurityException {
    String publicKey = new String(Files.readAllBytes(publicKeyFile.toPath()), US_ASCII);
    return loadPublicKey(publicKey);
  }

  public static PublicKey loadPublicKey(String publicKey) throws GeneralSecurityException {
    Matcher matcher = PUBLIC_KEY_PATTERN.matcher(publicKey);
    if (!matcher.find()) {
      throw new KeyStoreException("did not find a public key");
    }
    String data = matcher.group(1);
    byte[] encodedKey = base64Decode(data);

    X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(encodedKey);
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(encodedKeySpec);
    } catch (InvalidKeySpecException ignore) {
      // ignore
    }

    try {
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      return keyFactory.generatePublic(encodedKeySpec);
    } catch (InvalidKeySpecException ignore) {
      // ignore
    }

    KeyFactory keyFactory = KeyFactory.getInstance("DSA");
    return keyFactory.generatePublic(encodedKeySpec);
  }

  private static byte[] base64Decode(String base64) {
    return getMimeDecoder().decode(base64.getBytes(US_ASCII));
  }
}

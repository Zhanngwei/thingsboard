/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.transport.config.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `PemSslCredentials` 是 ThingsBoard Common Transport 中负责 `Pem Ssl Credentials` 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `AbstractSslCredentials`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PemSslCredentials extends AbstractSslCredentials {

    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String DEFAULT_KEY_ALIAS = "server";

    /**
     * 文件，用于定位本地文件或目录。
     */
    private String certFile;
    private String keyFile;
    /**
     * 密码，用于定位映射、配置或数据项。
     */
    private String keyPassword;

    /**
     * 功能：执行 `canUse` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean canUse() {
        return ResourceUtils.resourceExists(this, this.certFile);
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `trustsOnly`：`trustsOnly` 参数。
     * - `keyPasswordArray`：键。
     * 返回：处理结果。
     */
    @Override
    protected KeyStore loadKeyStore(boolean trustsOnly, char[] keyPasswordArray) throws IOException, GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        List<X509Certificate> certificates = new ArrayList<>();
        PrivateKey privateKey = null;
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
        try (InputStream inStream = ResourceUtils.getInputStream(this, this.certFile)) {
            try (PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {
                Object object;
                while((object = pemParser.readObject()) != null) {
                    if (object instanceof X509CertificateHolder) {
                        X509Certificate x509Cert = certConverter.getCertificate((X509CertificateHolder) object);
                        certificates.add(x509Cert);
                    } else if (object instanceof PEMEncryptedKeyPair) {
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                        privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                    } else if (object instanceof PEMKeyPair) {
                        privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                    } else if (object instanceof PrivateKeyInfo) {
                        privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                    }
                }
            }
        }
        if (privateKey == null && !StringUtils.isEmpty(this.keyFile)) {
            if (ResourceUtils.resourceExists(this, this.keyFile)) {
                try (InputStream inStream = ResourceUtils.getInputStream(this, this.keyFile)) {
                    try (PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {
                        Object object;
                        while ((object = pemParser.readObject()) != null) {
                            if (object instanceof PEMEncryptedKeyPair) {
                                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                                privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                                break;
                            } else if (object instanceof PEMKeyPair) {
                                privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                                break;
                            } else if (object instanceof PrivateKeyInfo) {
                                privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                            }
                        }
                    }
                }
            }
        }
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("No certificates found in certFile: " + this.certFile);
        }
        if (privateKey == null && !trustsOnly) {
            throw new IllegalArgumentException("Unable to load private key neither from certFile: " + this.certFile + " nor from keyFile: " + this.keyFile);
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        if (trustsOnly) {
            List<Certificate> unique = certificates.stream().distinct().collect(Collectors.toList());
            for (int i = 0; i < unique.size(); i++) {
                keyStore.setCertificateEntry("root-" + i, unique.get(i));
            }
        }
        if (privateKey != null) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            CertPath certPath = factory.generateCertPath(certificates);
            List<? extends Certificate> path = certPath.getCertificates();
            Certificate[] x509Certificates = path.toArray(new Certificate[0]);
            keyStore.setKeyEntry(DEFAULT_KEY_ALIAS, privateKey, keyPasswordArray, x509Certificates);
        }
        return keyStore;
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getKeyAlias() {
        return DEFAULT_KEY_ALIAS;
    }

    /**
     * 功能：更新键。
     * 参数：
     * - `keyAlias`：键。
     * 返回：无。
     */
    @Override
    protected void updateKeyAlias(String keyAlias) {
    }
}

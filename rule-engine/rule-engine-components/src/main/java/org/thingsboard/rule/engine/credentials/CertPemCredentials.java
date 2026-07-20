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
package org.thingsboard.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.SslUtil;
import org.thingsboard.server.common.data.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `CertPemCredentials` 是 ThingsBoard Rule Engine Components 中围绕 `Cert Pem Credentials` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `ClientCredentials`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertPemCredentials implements ClientCredentials {

    /**
     * 私钥常量，用于统一引用固定值。
     */
    public static final String PRIVATE_KEY_ALIAS = "private-key";
    /**
     * `X_509`常量，用于统一引用固定值。
     */
    public static final String X_509 = "X.509";
    /**
     * `CERT_ALIAS_PREFIX`常量，用于统一引用固定值。
     */
    public static final String CERT_ALIAS_PREFIX = "cert-";
    /**
     * `CA_CERT_CERT_ALIAS_PREFIX`常量，用于统一引用固定值。
     */
    public static final String CA_CERT_CERT_ALIAS_PREFIX = "caCert-cert-";

    /**
     * `caCert` 字段，保存当前对象的对应属性。
     */
    protected String caCert;
    /**
     * `cert` 字段，保存当前对象的对应属性。
     */
    private String cert;
    /**
     * 私钥，用于定位映射、配置或数据项。
     */
    private String privateKey;
    /**
     * 密码，用于认证或安全校验。
     */
    private String password = "";

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CredentialsType getType() {
        return CredentialsType.CERT_PEM;
    }

    /**
     * 功能：初始化或启动上下文。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SslContext initSslContext() {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();
            if (StringUtils.hasLength(caCert)) {
                builder.trustManager(createAndInitTrustManagerFactory());
            }
            if (StringUtils.hasLength(cert) && StringUtils.hasLength(privateKey)) {
                builder.keyManager(createAndInitKeyManagerFactory());
            }
            return builder.build();
        } catch (Exception e) {
            log.error("[{}:{}] Creating TLS factory failed!", caCert, cert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    /**
     * 功能：保存或创建工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    protected TrustManagerFactory createAndInitTrustManagerFactory() throws Exception {
        List<X509Certificate> caCerts = SslUtil.readCertFile(caCert);

        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        for (X509Certificate caCert : caCerts) {
            caKeyStore.setCertificateEntry(CA_CERT_CERT_ALIAS_PREFIX + caCert.getSubjectDN().getName(), caCert);
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory;
    }

    /**
     * 功能：保存或创建键。
     * 参数：无。
     * 返回：处理结果。
     */
    private KeyManagerFactory createAndInitKeyManagerFactory() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(loadKeyStore(), SslUtil.getPassword(password));
        return kmf;
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：处理结果。
     */
    protected KeyStore loadKeyStore() throws Exception {
        List<X509Certificate> certificates = SslUtil.readCertFile(this.cert);
        PrivateKey privateKey = SslUtil.readPrivateKey(this.privateKey, password);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        List<X509Certificate> unique = certificates.stream().distinct().collect(Collectors.toList());
        for (X509Certificate cert : unique) {
            keyStore.setCertificateEntry(CERT_ALIAS_PREFIX + cert.getSubjectDN().getName(), cert);
        }

        if (privateKey != null) {
            CertificateFactory factory = CertificateFactory.getInstance(X_509);
            CertPath certPath = factory.generateCertPath(certificates);
            List<? extends Certificate> path = certPath.getCertificates();
            Certificate[] x509Certificates = path.toArray(new Certificate[0]);
            keyStore.setKeyEntry(PRIVATE_KEY_ALIAS, privateKey, SslUtil.getPassword(password), x509Certificates);
        }
        return keyStore;
    }
}

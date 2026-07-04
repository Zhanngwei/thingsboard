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

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * 中文说明：`CertPemCredentials` 是证书PEM凭据辅助类，用于描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class CertPemCredentials implements ClientCredentials {

    /**
     * 常量字段：定义 `PRIVATE_KEY_ALIAS`，用于消息体、元数据、属性或遥测中的键名，本身不触发外部系统调用。
     */
    public static final String PRIVATE_KEY_ALIAS = "private-key";
    /**
     * 常量字段：定义 `X_509`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    public static final String X_509 = "X.509";
    /**
     * 常量字段：定义 `CERT_ALIAS_PREFIX`，用于证书内容，本身不触发外部系统调用。
     */
    public static final String CERT_ALIAS_PREFIX = "cert-";
    /**
     * 常量字段：定义 `CA_CERT_CERT_ALIAS_PREFIX`，用于证书内容，本身不触发外部系统调用。
     */
    public static final String CA_CERT_CERT_ALIAS_PREFIX = "caCert-cert-";

    /**
     * 字段说明：保存 `caCert`，表示证书内容，供本类方法在规则节点处理流程中使用。
     */
    protected String caCert;
    /**
     * 字段说明：保存 `cert`，表示证书内容，供本类方法在规则节点处理流程中使用。
     */
    private String cert;
    /**
     * 字段说明：保存 `privateKey`，表示消息体、元数据、属性或遥测中的键名，供本类方法在规则节点处理流程中使用。
     */
    private String privateKey;
    /**
     * 字段说明：保存 `password`，表示凭据密码，供本类方法在规则节点处理流程中使用。
     */
    private String password = "";

    @Override
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `CertPemCredentials` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public CredentialsType getType() {
        return CredentialsType.CERT_PEM;
    }

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态，供 `CertPemCredentials` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
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
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态，供 `CertPemCredentials` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态，供 `CertPemCredentials` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private KeyManagerFactory createAndInitKeyManagerFactory() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(loadKeyStore(), SslUtil.getPassword(password));
        return kmf;
    }

    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `CertPemCredentials` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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

    /*
     * 本类总结：`CertPemCredentials` 负责描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}

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

import org.thingsboard.server.common.data.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`AbstractSslCredentials` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
public abstract class AbstractSslCredentials implements SslCredentials {

    /**
     * 密码列表，用于保存一组待处理对象。
     */
    private char[] keyPasswordArray;

    /**
     * 键，用于定位映射、配置或数据项。
     */
    private KeyStore keyStore;

    /**
     * 私钥，用于定位映射、配置或数据项。
     */
    private PrivateKey privateKey;

    /**
     * 公钥，用于定位映射、配置或数据项。
     */
    private PublicKey publicKey;

    /**
     * `chain`列表，用于保存一组待处理对象。
     */
    private X509Certificate[] chain;

    /**
     * `trusts`列表，用于保存一组待处理对象。
     */
    private X509Certificate[] trusts;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `trustsOnly`：`trustsOnly` 参数。
     * 返回：无。
     */
    @Override
    public void init(boolean trustsOnly) throws IOException, GeneralSecurityException {
        String keyPassword = getKeyPassword();
        if (StringUtils.isEmpty(keyPassword)) {
            this.keyPasswordArray = new char[0];
        } else {
            this.keyPasswordArray = keyPassword.toCharArray();
        }
        this.keyStore = this.loadKeyStore(trustsOnly, this.keyPasswordArray);
        Set<X509Certificate> trustedCerts = getTrustedCerts(this.keyStore, trustsOnly);
        this.trusts = trustedCerts.toArray(new X509Certificate[0]);
        if (!trustsOnly) {
            PrivateKeyEntry privateKeyEntry = null;
            String keyAlias = this.getKeyAlias();
            if (!StringUtils.isEmpty(keyAlias)) {
                privateKeyEntry = tryGetPrivateKeyEntry(this.keyStore, keyAlias, this.keyPasswordArray);
            } else {
                for (Enumeration<String> e = this.keyStore.aliases(); e.hasMoreElements(); ) {
                    String alias = e.nextElement();
                    privateKeyEntry = tryGetPrivateKeyEntry(this.keyStore, alias, this.keyPasswordArray);
                    if (privateKeyEntry != null) {
                        this.updateKeyAlias(alias);
                        break;
                    }
                }
            }
            if (privateKeyEntry == null) {
                throw new IllegalArgumentException("Failed to get private key from the keystore or pem files. " +
                        "Please check if the private key exists in the keystore or pem files and if the provided private key password is valid.");
            }
            this.chain = asX509Certificates(privateKeyEntry.getCertificateChain());
            this.privateKey = privateKeyEntry.getPrivateKey();
            if (this.chain.length > 0) {
                this.publicKey = this.chain[0].getPublicKey();
            }
        }
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public KeyStore getKeyStore() {
        return this.keyStore;
    }

    /**
     * 功能：获取私钥。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    /**
     * 功能：获取公钥。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    /**
     * 功能：获取证书。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public X509Certificate[] getCertificateChain() {
        return this.chain;
    }

    /**
     * 功能：获取`Trusted Certificates`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public X509Certificate[] getTrustedCertificates() {
        return this.trusts;
    }

    /**
     * 功能：保存或创建工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(this.keyStore);
        return tmFactory;
    }

    /**
     * 功能：保存或创建键。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(this.keyStore, this.keyPasswordArray);
        return kmf;
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `subjectName`：名称。
     * - `key`：键。
     * 返回：文本结果。
     */
    @Override
    public String getValueFromSubjectNameByKey(String subjectName, String key) {
        String[] dns = subjectName.split(",");
        Optional<String> cn = (Arrays.stream(dns).filter(dn -> dn.contains(key + "="))).findFirst();
        String value = cn.isPresent() ? cn.get().replace(key + "=", "") : null;
        return StringUtils.isNotEmpty(value) ? value : null;
    }

    /**
     * 功能：执行 `canUse` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    protected abstract boolean canUse();

    /**
     * 功能：获取键。
     * 参数：
     * - `isPrivateKeyRequired`：键。
     * - `keyPasswordArray`：键。
     * 返回：处理结果。
     */
    protected abstract KeyStore loadKeyStore(boolean isPrivateKeyRequired, char[] keyPasswordArray) throws IOException, GeneralSecurityException;

    /**
     * 功能：更新键。
     * 参数：
     * - `keyAlias`：键。
     * 返回：无。
     */
    protected abstract void updateKeyAlias(String keyAlias);

    /**
     * 功能：执行 `asX509Certificates` 对应的处理。
     * 参数：
     * - `certificates`：`certificates` 参数。
     * 返回：处理结果。
     */
    private static X509Certificate[] asX509Certificates(Certificate[] certificates) {
        if (null == certificates || 0 == certificates.length) {
            throw new IllegalArgumentException("certificates missing!");
        }
        X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
        for (int index = 0; certificates.length > index; ++index) {
            if (null == certificates[index]) {
                throw new IllegalArgumentException("[" + index + "] is null!");
            }
            try {
                x509Certificates[index] = (X509Certificate) certificates[index];
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("[" + index + "] is not a x509 certificate! Instead it's a "
                        + certificates[index].getClass().getName());
            }
        }
        return x509Certificates;
    }

    /**
     * 功能：执行 `tryGetPrivateKeyEntry` 对应的处理。
     * 参数：
     * - `keyStore`：键。
     * - `alias`：`alias` 参数。
     * - `pwd`：`pwd` 参数。
     * 返回：处理结果。
     */
    private static PrivateKeyEntry tryGetPrivateKeyEntry(KeyStore keyStore, String alias, char[] pwd) {
        PrivateKeyEntry entry = null;
        try {
            if (keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                try {
                    entry = (KeyStore.PrivateKeyEntry) keyStore
                            .getEntry(alias, new KeyStore.PasswordProtection(pwd));
                } catch (UnsupportedOperationException e) {
                    PrivateKey key = (PrivateKey) keyStore.getKey(alias, pwd);
                    Certificate[] certs = keyStore.getCertificateChain(alias);
                    entry = new KeyStore.PrivateKeyEntry(key, certs);
                }
            }
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException ignored) {}
        return entry;
    }

    /**
     * 功能：获取`Trusted Certs`。
     * 参数：
     * - `ks`：`ks` 参数。
     * - `trustsOnly`：`trustsOnly` 参数。
     * 返回：匹配的数据集合。
     */
    private static Set<X509Certificate> getTrustedCerts(KeyStore ks, boolean trustsOnly) {
        Set<X509Certificate> set = new HashSet<>();
        try {
            for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                if (ks.isCertificateEntry(alias)) {
                    Certificate cert = ks.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        if (trustsOnly) {
                            // is CA certificate
                            if (((X509Certificate) cert).getBasicConstraints()>=0) {
                                set.add((X509Certificate) cert);
                            }
                        } else {
                            set.add((X509Certificate) cert);
                        }
                    }
                } else if (ks.isKeyEntry(alias)) {
                    Certificate[] certs = ks.getCertificateChain(alias);
                    if ((certs != null) && (certs.length > 0) &&
                            (certs[0] instanceof X509Certificate)) {
                        if (trustsOnly) {
                            for (Certificate cert : certs) {
                                // is CA certificate
                                if (((X509Certificate) cert).getBasicConstraints()>=0) {
                                    set.add((X509Certificate) cert);
                                }
                            }
                        } else {
                            set.add((X509Certificate)certs[0]);
                        }
                    }
                }
            }
        } catch (KeyStoreException ignored) {}
        return Collections.unmodifiableSet(set);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractSslCredentials` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

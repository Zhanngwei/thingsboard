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

@Data
@EqualsAndHashCode(callSuper = false)
/**
 * 中文说明：
 * 1. 类目的：`PemSslCredentials` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
public class PemSslCredentials extends AbstractSslCredentials {

    /**
     * 字段说明：
     * 1. 保存 `DEFAULT_KEY_ALIAS` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String DEFAULT_KEY_ALIAS = "server";

    /**
     * 字段说明：
     * 1. 保存 `certFile` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String certFile;
    private String keyFile;
    /**
     * 字段说明：
     * 1. 保存 `keyPassword` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String keyPassword;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `canUse` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean canUse() {
        return ResourceUtils.resourceExists(this, this.certFile);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `loadKeyStore` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected KeyStore loadKeyStore(boolean trustsOnly, char[] keyPasswordArray) throws IOException, GeneralSecurityException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (object instanceof X509CertificateHolder) {
                        X509Certificate x509Cert = certConverter.getCertificate((X509CertificateHolder) object);
                        certificates.add(x509Cert);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    } else if (object instanceof PEMEncryptedKeyPair) {
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                        privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    } else if (object instanceof PEMKeyPair) {
                        privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    } else if (object instanceof PrivateKeyInfo) {
                        privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                    }
                }
            }
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (privateKey == null && !StringUtils.isEmpty(this.keyFile)) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (ResourceUtils.resourceExists(this, this.keyFile)) {
                try (InputStream inStream = ResourceUtils.getInputStream(this, this.keyFile)) {
                    try (PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {
                        Object object;
                        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                        while ((object = pemParser.readObject()) != null) {
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            if (object instanceof PEMEncryptedKeyPair) {
                                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                                privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                                break;
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            } else if (object instanceof PEMKeyPair) {
                                privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                                break;
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            } else if (object instanceof PrivateKeyInfo) {
                                privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                            }
                        }
                    }
                }
            }
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getKeyAlias` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getKeyAlias() {
        return DEFAULT_KEY_ALIAS;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateKeyAlias` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void updateKeyAlias(String keyAlias) {
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`PemSslCredentials` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

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
package org.thingsboard.server.transport.lwm2m.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Assert;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509BootstrapClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.PSKLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.RPKLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.X509LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.client.object.Security.noSecBootstap;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_DEREGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;

@DaoSqlTest
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractSecurityLwM2MIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractSecurityLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    /**
     * 字段说明：
     * 1. 保存 `CREDENTIALS_PATH` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final String CREDENTIALS_PATH = "lwm2m/credentials/";                              // client public key or id used for PSK
    //             Get keys PSK
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_PSK_IDENTITY` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final String CLIENT_PSK_IDENTITY = "SOME_PSK_ID";                                  // client public key or id used for PSK
    protected final String CLIENT_PSK_IDENTITY_BS = "SOME_PSK_ID_BS";                            // client public key or id used for PSK
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_PSK_KEY` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final String CLIENT_PSK_KEY = "73656372657450534b73656372657450";                  // client private/secret key used for PSK

    // Server
    /**
     * 字段说明：
     * 1. 保存 `SERVER_JKS_FOR_TEST` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String SERVER_JKS_FOR_TEST = "lwm2mserver";
    protected static final String SERVER_STORE_PWD = "server_ks_password";
    /**
     * 字段说明：
     * 1. 保存 `SERVER_CERT_ALIAS` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String SERVER_CERT_ALIAS = "server";
    protected static final String SERVER_CERT_ALIAS_BS = "bootstrap";
    /**
     * 字段说明：
     * 1. 保存 `serverX509Cert;` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final X509Certificate serverX509Cert;                                               // server certificate signed by rootCA
    protected final X509Certificate serverX509CertBs;                                             // serverBs certificate signed by rootCA
    /**
     * 字段说明：
     * 1. 保存 `serverPublicKeyFromCert;` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final PublicKey serverPublicKeyFromCert;                                            // server public key used for RPK
    protected final PublicKey serverPublicKeyFromCertBs;                                          // serverBs public key used for RPK

    // Client
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_ENDPOINT_NO_SEC` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String CLIENT_ENDPOINT_NO_SEC = "LwNoSec00000000";
    protected static final String CLIENT_ENDPOINT_NO_SEC_BS = "LwNoSecBs00000000";
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_ENDPOINT_PSK` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String CLIENT_ENDPOINT_PSK = "LwPsk00000000";
    protected static final String CLIENT_ENDPOINT_PSK_BS = "LwPskBs00000000";
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_ENDPOINT_RPK` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String CLIENT_ENDPOINT_RPK = "LwRpk00000000";
    protected static final String CLIENT_ENDPOINT_RPK_BS = "LwRpkBs00000000";
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_ENDPOINT_X509_TRUST` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String CLIENT_ENDPOINT_X509_TRUST = "LwX50900000000";
    protected static final String CLIENT_ENDPOINT_X509_TRUST_NO = "LwX509TrustNo";
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_JKS_FOR_TEST` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String CLIENT_JKS_FOR_TEST = "lwm2mclient";
    protected static final String CLIENT_STORE_PWD = "client_ks_password";
    /**
     * 字段说明：
     * 1. 保存 `CLIENT_ALIAS_CERT_TRUST` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String CLIENT_ALIAS_CERT_TRUST = "client_alias_00000000";
    protected static final String CLIENT_ALIAS_CERT_TRUST_NO = "client_alias_trust_no";

    /**
     * 字段说明：
     * 1. 保存 `clientX509CertTrust;` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final X509Certificate clientX509CertTrust;                                        // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrust;                                   // client private key used for X509 and RPK
    /**
     * 字段说明：
     * 1. 保存 `clientX509CertTrustNo;` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final X509Certificate clientX509CertTrustNo;                                      // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrustNo;                                 // client private key used for X509 and RPK
    /**
     * 字段说明：
     * 1. 保存 `RESOURCES_SECURITY` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final String[] RESOURCES_SECURITY = new String[]{"1.xml", "2.xml", "3.xml", "5.xml", "9.xml"};


    /**
     * 字段说明：
     * 1. 保存 `defaultBootstrapCredentials` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2MBootstrapClientCredentials defaultBootstrapCredentials;


    /**
     * 方法说明：
     * 1. 职责：执行 `AbstractSecurityLwM2MIntegrationTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public AbstractSecurityLwM2MIntegrationTest() {
        // create client credentials
        setResources(this.RESOURCES_SECURITY);
        try {
            // Get certificates from key store
            char[] clientKeyStorePwd = CLIENT_STORE_PWD.toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream clientKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + CLIENT_JKS_FOR_TEST + ".jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }
            // Trust
            clientPrivateKeyFromCertTrust = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST, clientKeyStorePwd);
            clientX509CertTrust = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST);
            // No trust
            clientPrivateKeyFromCertTrustNo = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST_NO, clientKeyStorePwd);
            clientX509CertTrustNo = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST_NO);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Get certificates from key store
            char[] serverKeyStorePwd = SERVER_STORE_PWD.toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream serverKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + SERVER_JKS_FOR_TEST + ".jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }

            serverX509Cert = (X509Certificate) serverKeyStore.getCertificate(SERVER_CERT_ALIAS);
            serverPublicKeyFromCert = serverX509Cert.getPublicKey();
            serverX509CertBs = (X509Certificate) serverKeyStore.getCertificate(SERVER_CERT_ALIAS_BS);
            serverPublicKeyFromCertBs = serverX509CertBs.getPublicKey();

        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        defaultBootstrapCredentials = new LwM2MBootstrapClientCredentials();

        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();

        defaultBootstrapCredentials.setBootstrapServer(serverCredentials);
        defaultBootstrapCredentials.setLwm2mServer(serverCredentials);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `basicTestConnectionBefore` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void basicTestConnectionBefore(String clientEndpoint,
                                          String awaitAlias,
                                          LwM2MProfileBootstrapConfigType type,
                                          Set<LwM2MClientState> expectedStatuses,
                                          LwM2MClientState finishState) throws Exception {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(type));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        this.basicTestConnection(noSecBootstap(URI_BS),
                deviceCredentials,
                COAP_CONFIG_BS,
                clientEndpoint,
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                transportConfiguration,
                awaitAlias,
                expectedStatuses,
                true,
                finishState,
                false);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `basicTestConnection` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void basicTestConnection(Security security,
                                       LwM2MDeviceCredentials deviceCredentials,
                                       Configuration coapConfig,
                                       String endpoint,
                                       Lwm2mDeviceProfileTransportConfiguration transportConfiguration,
                                       String awaitAlias,
                                       Set<LwM2MClientState> expectedStatuses,
                                       boolean isBootstrap,
                                       LwM2MClientState finishState,
                                       boolean isStartLw) throws Exception {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        createDeviceProfile(transportConfiguration);
        final Device device = createDevice(deviceCredentials, endpoint);
        device.getId().getId().toString();
        createNewClient(security, coapConfig, true, endpoint, isBootstrap, null);
        lwM2MTestClient.start(isStartLw);
        awaitObserveReadAll(0, isBootstrap, device.getId().getId().toString());
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTestConnection started -> finishState: [{}] states: {}", finishState, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(finishState) || lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_STARTED);
                });
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTestConnection -> finishState: [{}] states: {}", finishState, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(finishState) || lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS);
                });
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatuses));
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `basicTestConnectionBootstrapRequestTriggerBefore` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void basicTestConnectionBootstrapRequestTriggerBefore(String clientEndpoint, String awaitAlias, LwM2MProfileBootstrapConfigType type) throws Exception {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(type));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        this.basicTestConnectionBootstrapRequestTrigger(
                SECURITY_NO_SEC,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccess,
                expectedStatusesRegistrationBsSuccess,
                false,
                SECURITY_NO_SEC_BS);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `basicTestConnectionBootstrapRequestTrigger` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void basicTestConnectionBootstrapRequestTrigger(Security security,
                                                            LwM2MDeviceCredentials deviceCredentials,
                                                            Configuration coapConfig,
                                                            String endpoint,
                                                            Lwm2mDeviceProfileTransportConfiguration transportConfiguration,
                                                            String awaitAlias,
                                                            Set<LwM2MClientState> expectedStatusesLwm2m,
                                                            Set<LwM2MClientState> expectedStatusesBs,
                                                            boolean isBootstrap,
                                                            Security securityBs) throws Exception {

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        createDeviceProfile(transportConfiguration);
        final Device device = createDevice(deviceCredentials, endpoint);
        String deviceIdStr = device.getId().getId().toString();
        createNewClient(security, coapConfig, true, endpoint, isBootstrap, securityBs);
        lwM2MTestClient.start(true);
        awaitObserveReadAll(0, isBootstrap, deviceIdStr);
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTest First Connection started -> finishState: [{}] states: {}", ON_REGISTRATION_SUCCESS, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_SUCCESS) || lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_STARTED);
                });
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTest First  Connection -> finishState: [{}] states: {}", ON_REGISTRATION_SUCCESS, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_SUCCESS) || lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS);
                });
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesLwm2m));

        String executedPath = "/" + OBJECT_ID_1 + "_" +  lwM2MTestClient.getLeshanClient().getObjectTree().getModel().getObjectModel(OBJECT_ID_1).version
                + "/0/" + RESOURCE_ID_9;
        lwM2MTestClient.setClientStates(new HashSet<>());
        String actualResult = sendRPCSecurityExecuteById(executedPath, deviceIdStr, endpoint);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!(rpcActualResult.get("result").asText().equals(ResponseCode.CHANGED.getName()))) {
            actualResult = sendRPCSecurityExecuteById(executedPath, deviceIdStr, endpoint);
            rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        }
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
        expectedStatusesBs.add(ON_DEREGISTRATION_STARTED);
        expectedStatusesBs.add(ON_DEREGISTRATION_SUCCESS);
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTestConnection started -> finishState: [{}] states: {}", ON_REGISTRATION_SUCCESS, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_SUCCESS) || lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_STARTED);
                });
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTestConnection -> finishState: [{}] states: {}", ON_REGISTRATION_SUCCESS, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_SUCCESS) || lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS);
                });
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesBs));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getBootstrapServerCredentialsSecure` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected List<LwM2MBootstrapServerCredential> getBootstrapServerCredentialsSecure(LwM2MSecurityMode mode, LwM2MProfileBootstrapConfigType bootstrapConfigType) {
        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (bootstrapConfigType) {
            case BOTH:
                bootstrap.add(getBootstrapServerCredential(mode, false));
                bootstrap.add(getBootstrapServerCredential(mode, true));
                break;
            case BOOTSTRAP_ONLY:
                bootstrap.add(getBootstrapServerCredential(mode, true));
                break;
            case LWM2M_ONLY:
                bootstrap.add(getBootstrapServerCredential(mode, false));
                break;
            case NONE:
        }
        return bootstrap;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getBootstrapServerCredential` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private AbstractLwM2MBootstrapServerCredential getBootstrapServerCredential(LwM2MSecurityMode mode, boolean isBootstrap) {
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential;
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (mode) {
            case PSK:
                bootstrapServerCredential = new PSKLwM2MBootstrapServerCredential();
                bootstrapServerCredential.setServerPublicKey("");
                break;
            case RPK:
                bootstrapServerCredential = new RPKLwM2MBootstrapServerCredential();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (isBootstrap) {
                    bootstrapServerCredential.setServerPublicKey(Base64.encodeBase64String(serverPublicKeyFromCertBs.getEncoded()));
                } else {
                    bootstrapServerCredential.setServerPublicKey(Base64.encodeBase64String(serverPublicKeyFromCert.getEncoded()));
                }
                break;
            case X509:
                bootstrapServerCredential = new X509LwM2MBootstrapServerCredential();
                try {
                    if (isBootstrap) {
                        bootstrapServerCredential.setServerPublicKey(Base64.encodeBase64String(serverX509CertBs.getEncoded()));
                    } else {
                        bootstrapServerCredential.setServerPublicKey(Base64.encodeBase64String(serverX509Cert.getEncoded()));
                    }
                } catch (CertificateEncodingException e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
        bootstrapServerCredential.setShortServerId(isBootstrap ? shortServerIdBs : shortServerId);
        bootstrapServerCredential.setBootstrapServerIs(isBootstrap);
        bootstrapServerCredential.setHost(isBootstrap ? hostBs : host);
        bootstrapServerCredential.setPort(isBootstrap ? securityPortBs : securityPort);
        return bootstrapServerCredential;
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceCredentialsSecure` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected LwM2MDeviceCredentials getDeviceCredentialsSecure(LwM2MClientCredential clientCredentials,
                                                                PrivateKey privateKey,
                                                                X509Certificate certificate,
                                                                LwM2MSecurityMode mode,
                                                                boolean privateKeyIsBad) {
        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        LwM2MBootstrapClientCredentials bootstrapCredentials;
        switch (mode) {
            case PSK:
                bootstrapCredentials = getBootstrapClientCredentialsPsk(clientCredentials);
                break;
            case RPK:
                bootstrapCredentials = getBootstrapClientCredentialsRpk(certificate, privateKey, privateKeyIsBad);
                break;
            case X509:
                bootstrapCredentials = getBootstrapClientCredentialsX509(certificate, privateKey, privateKeyIsBad);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getBootstrapClientCredentialsPsk` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MBootstrapClientCredentials getBootstrapClientCredentialsPsk(LwM2MClientCredential clientCredentials) {
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        PSKBootstrapClientCredential serverCredentials = new PSKBootstrapClientCredential();
        if (clientCredentials != null) {
            serverCredentials.setClientSecretKey(((PSKClientCredential) clientCredentials).getKey());
            serverCredentials.setClientPublicKeyOrId(((PSKClientCredential) clientCredentials).getIdentity());
        }
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        return bootstrapCredentials;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getBootstrapClientCredentialsRpk` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MBootstrapClientCredentials getBootstrapClientCredentialsRpk(X509Certificate certificate, PrivateKey privateKey, boolean privateKeyIsBad) {
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        RPKBootstrapClientCredential serverCredentials = new RPKBootstrapClientCredential();
        if (certificate != null && certificate.getPublicKey() != null && privateKey != null) {
            serverCredentials.setClientPublicKeyOrId(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
            if (privateKeyIsBad) {
                serverCredentials.setClientSecretKey(Hex.encodeHexString(privateKey.getEncoded()));
            } else {
                serverCredentials.setClientSecretKey(Base64.encodeBase64String(privateKey.getEncoded()));

            }
        }
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        return bootstrapCredentials;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getBootstrapClientCredentialsX509` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MBootstrapClientCredentials getBootstrapClientCredentialsX509(X509Certificate certificate, PrivateKey privateKey, boolean privateKeyIsBad) {
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        X509BootstrapClientCredential serverCredentials = new X509BootstrapClientCredential();
        if (certificate != null) {
            try {
                serverCredentials.setClientPublicKeyOrId(Base64.encodeBase64String(certificate.getEncoded()));
                if (privateKeyIsBad) {
                    serverCredentials.setClientSecretKey(Hex.encodeHexString(privateKey.getEncoded()));
                } else {
                    serverCredentials.setClientSecretKey(Base64.encodeBase64String(privateKey.getEncoded()));
                }
            } catch (CertificateEncodingException e) {
                log.error("Client`s certificate [{}] is bad. [{}]", certificate, e.getMessage());
            }
        }
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        return bootstrapCredentials;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDeviceWithMvcResult` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected MvcResult createDeviceWithMvcResult(LwM2MDeviceCredentials credentials, String endpoint) throws Exception {
        Device device = new Device();
        device.setName(endpoint);
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        return doPost("/api/device/credentials", deviceCredentials).andReturn();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendRPCSecurityExecuteById` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String sendRPCSecurityExecuteById(String path, String deviceId, String endpoint) throws Exception {
        log.info("endpoint1: [{}]", endpoint);


        String setRpcRequest = "{\"method\": \"Execute\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractSecurityLwM2MIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

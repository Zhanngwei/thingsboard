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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PreDestroy;
import java.security.cert.X509Certificate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CURVES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MNetworkConfig.getCoapConfig;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE;

@Slf4j
@Component
@DependsOn({"lwM2mDownlinkMsgHandler", "lwM2mUplinkMsgHandler"})
@TbLwM2mTransportComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultLwM2mTransportService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class DefaultLwM2mTransportService implements LwM2MTransportService {

    /**
     * 字段说明：
     * 1. 保存 `RPK_OR_X509_CIPHER_SUITES` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final CipherSuite[] RPK_OR_X509_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256};
    public static final CipherSuite[] PSK_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256};

    /**
     * 字段说明：
     * 1. 保存 `context` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    /**
     * 字段说明：
     * 1. 保存 `otaPackageDataCache` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final OtaPackageDataCache otaPackageDataCache;
    private final LwM2mUplinkMsgHandler handler;
    /**
     * 字段说明：
     * 1. 保存 `registrationStore` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final CaliforniumRegistrationStore registrationStore;
    private final TbSecurityStore securityStore;
    /**
     * 字段说明：
     * 1. 保存 `certificateVerifier` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbLwM2MDtlsCertificateVerifier certificateVerifier;
    private final TbLwM2MAuthorizer authorizer;
    /**
     * 字段说明：
     * 1. 保存 `modelProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mVersionedModelProvider modelProvider;

    /**
     * 字段说明：
     * 1. 保存 `server` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LeshanServer server;

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init() {
        this.server = getLhServer();
        /*
         * Add a resource to the server.
         * CoapResource ->
         * path = FW_PACKAGE or SW_PACKAGE
         * nameFile = "BC68JAR01A09_TO_BC68JAR01A10.bin"
         * "coap://host:port/{path}/{token}/{nameFile}"
         */
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        LwM2mTransportCoapResource otaCoapResource = new LwM2mTransportCoapResource(otaPackageDataCache, FIRMWARE_UPDATE_COAP_RESOURCE);
        this.server.coap().getServer().add(otaCoapResource);
        this.context.setServer(server);
        this.startLhServer();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startLhServer` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void startLhServer() {
        log.info("Starting LwM2M transport server...");
        this.server.start();
        LwM2mServerListener lhServerCertListener = new LwM2mServerListener(handler);
        this.server.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.server.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.server.getObservationService().addListener(lhServerCertListener.observationListener);
        this.server.getSendService().addListener(lhServerCertListener.sendListener);
        log.info("Started LwM2M transport server.");
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `shutdown` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void shutdown() {
        try {
            log.info("Stopping LwM2M transport server!");
            server.destroy();
            log.info("LwM2M transport server stopped!");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("Failed to gracefully stop the LwM2M transport server!", e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getLhServer` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LeshanServer getLhServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(config.getHost(), config.getPort());
        builder.setLocalSecureAddress(config.getSecureHost(), config.getSecurePort());
        builder.setDecoder(new DefaultLwM2mDecoder());
        /* Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mEncoder(LwM2mValueConverterImpl.getInstance()));

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        /* Define model provider (Create Models )*/
        builder.setObjectModelProvider(modelProvider);

        /* Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
        builder.setRegistrationStore(registrationStore);

        /* Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        dtlsConfig.set(DTLS_RECOMMENDED_CURVES_ONLY, config.isRecommendedSupportedGroups());
        dtlsConfig.set(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, config.isRecommendedCiphers());
        dtlsConfig.set(DTLS_RETRANSMISSION_TIMEOUT, config.getDtlsRetransmissionTimeout(), MILLISECONDS);
        dtlsConfig.set(DTLS_CONNECTION_ID_LENGTH, config.getDtlsConnectionIdLength());
        dtlsConfig.set(DTLS_ROLE, SERVER_ONLY);

        /*  Create credentials */
        this.setServerWithCredentials(builder, dtlsConfig);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /* Create LWM2M server */
        return builder.build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setServerWithCredentials` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setServerWithCredentials(LeshanServerBuilder builder, DtlsConnectorConfig.Builder dtlsConfig) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (this.config.getSslCredentials() != null) {
            SslCredentials sslCredentials = this.config.getSslCredentials();
            builder.setPublicKey(sslCredentials.getPublicKey());
            builder.setPrivateKey(sslCredentials.getPrivateKey());
            builder.setCertificateChain(sslCredentials.getCertificateChain());
            dtlsConfig.setAdvancedCertificateVerifier(certificateVerifier);
            builder.setAuthorizer(authorizer);
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, RPK_OR_X509_CIPHER_SUITES);
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
            log.info("Unable to load X509 files for LWM2MServer");
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, PSK_CIPHER_SUITES);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getName` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getName() {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        return DataConstants.LWM2M_TRANSPORT_NAME;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultLwM2mTransportService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

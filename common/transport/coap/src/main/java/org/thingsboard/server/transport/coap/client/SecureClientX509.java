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
package org.thingsboard.server.transport.coap.client;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. 类目的：`SecureClientX509` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SecureClientX509 {

    /**
     * 字段说明：
     * 1. 保存 `dtlsConnector` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final DTLSConnector dtlsConnector;
    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    /**
     * 字段说明：
     * 1. 保存 `coapClient` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private CoapClient coapClient;

    /**
     * 方法说明：
     * 1. 职责：执行 `SecureClientX509` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SecureClientX509(DTLSConnector dtlsConnector, String host, int port, String clientKeys, String sharedKeys) throws URISyntaxException {
        this.dtlsConnector = dtlsConnector;
        this.coapClient = getCoapClient(host, port, clientKeys, sharedKeys);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `test` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void test() {
        executor.submit(() -> {
            try {
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                while (!Thread.interrupted()) {
                    CoapResponse response = null;
                    try {
                        response = coapClient.get();
                    // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                    } catch (ConnectorException | IOException e) {
                        System.err.println("Error occurred while sending request: " + e);
                        System.exit(-1);
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (response != null) {

                        System.out.println(response.getCode() + " - " + response.getCode().name());
                        System.out.println(response.getOptions());
                        System.out.println(response.getResponseText());
                        System.out.println();
                        System.out.println("ADVANCED:");
                        EndpointContext context = response.advanced().getSourceContext();
                        Principal identity = context.getPeerIdentity();
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (identity != null) {
                            System.out.println(context.getPeerIdentity());
                        } else {
                            System.out.println("anonymous");
                        }
                        System.out.println(context.get(DtlsEndpointContext.KEY_CIPHER));
                        System.out.println(Utils.prettyPrint(response));
                    } else {
                        System.out.println("No response received.");
                    }
                    Thread.sleep(5000);
                }
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                System.out.println("Error occurred while sending COAP requests.");
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCoapClient` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private CoapClient getCoapClient(String host, Integer port, String clientKeys, String sharedKeys) throws URISyntaxException {
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        URI uri = new URI(getFutureUrl(host, port, clientKeys, sharedKeys));
        CoapClient client = new CoapClient(uri);
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(dtlsConnector);

        client.setEndpoint(builder.build());
        return client;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getFutureUrl` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getFutureUrl(String host, Integer port, String clientKeys, String sharedKeys) {
        return "coaps://" + host + ":" + port + "/api/v1/attributes?clientKeys=" + clientKeys + "&sharedKeys=" + sharedKeys;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `main` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static void main(String[] args) throws URISyntaxException {
        System.out.println("Usage: java -cp ... org.thingsboard.server.transport.coap.client.SecureClientX509 " +
                "host port keyStoreUriPath keyStoreAlias trustedAliasPattern clientKeys sharedKeys");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String clientKeys = args[6];
        String sharedKeys = args[7];

        String keyStoreUriPath = args[2];
        String keyStoreAlias = args[3];
        String trustedAliasPattern = args[4];
        String keyStorePassword = args[5];


        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new Configuration());
        setupCredentials(builder, keyStoreUriPath, keyStoreAlias, trustedAliasPattern, keyStorePassword);
        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        SecureClientX509 client = new SecureClientX509(dtlsConnector, host, port, clientKeys, sharedKeys);
        client.test();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setupCredentials` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void setupCredentials(DtlsConnectorConfig.Builder config, String keyStoreUriPath, String keyStoreAlias, String trustedAliasPattern, String keyStorePassword) {
        StaticNewAdvancedCertificateVerifier.Builder trustBuilder = StaticNewAdvancedCertificateVerifier.builder();
        try {
            SslContextUtil.Credentials serverCredentials = SslContextUtil.loadCredentials(
                    keyStoreUriPath, keyStoreAlias, keyStorePassword.toCharArray(), keyStorePassword.toCharArray());
            Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
                    keyStoreUriPath, trustedAliasPattern, keyStorePassword.toCharArray());
            trustBuilder.setTrustedCertificates(trustedCertificates);
            config.setAdvancedCertificateVerifier(trustBuilder.build());
            config.setCertificateIdentityProvider(new SingleCertificateProvider(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(), Collections.singletonList(CertificateType.X_509)));
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (GeneralSecurityException e) {
            System.err.println("certificates are invalid!");
            throw new IllegalArgumentException(e.getMessage());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (IOException e) {
            System.err.println("certificates are missing!");
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SecureClientX509` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

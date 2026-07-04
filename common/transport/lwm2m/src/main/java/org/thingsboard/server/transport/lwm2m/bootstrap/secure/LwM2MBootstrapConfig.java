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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.thingsboard.server.common.data.device.credentials.lwm2m.AbstractLwM2MBootstrapClientCredentialWithKeys;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;

import java.io.Serializable;
import java.util.List;

@Slf4j
@Data
/**
 * 中文说明：
 * 1. 类目的：`LwM2MBootstrapConfig` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2MBootstrapConfig implements Serializable {

    /**
     * 字段说明：
     * 1. 保存 `serialVersionUID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final long serialVersionUID = -4729088085817468640L;

    /**
     * 字段说明：
     * 1. 保存 `serverConfiguration` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    List<LwM2MBootstrapServerCredential> serverConfiguration;

    /** -bootstrapServer, lwm2mServer
     * interface ServerSecurityConfig
     *   host?: string,
     *   port?: number,
     *   isBootstrapServer?: boolean,
     *   securityMode: string,
     *   clientPublicKeyOrId?: string,
     *   clientSecretKey?: string,
     *   serverPublicKey?: string;
     *   clientHoldOffTime?: number,
     *   serverId?: number,
     *   bootstrapServerAccountTimeout: number
     * */
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `bootstrapServer` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2MBootstrapClientCredential bootstrapServer;

    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `lwm2mServer` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2MBootstrapClientCredential lwm2mServer;

    /**
     * 方法说明：
     * 1. 职责：执行 `LwM2MBootstrapConfig` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2MBootstrapConfig(){};

    /**
     * 方法说明：
     * 1. 职责：执行 `LwM2MBootstrapConfig` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2MBootstrapConfig(List<LwM2MBootstrapServerCredential> serverConfiguration, LwM2MBootstrapClientCredential bootstrapClientServer, LwM2MBootstrapClientCredential lwm2mClientServer) {
        this.serverConfiguration = serverConfiguration;
        this.bootstrapServer = bootstrapClientServer;
        this.lwm2mServer = lwm2mClientServer;

    }

    @JsonIgnore
    /**
     * 方法说明：
     * 1. 职责：执行 `getLwM2MBootstrapConfig` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapConfig getLwM2MBootstrapConfig() {
        BootstrapConfig configBs = new BootstrapConfig();
        configBs.autoIdForSecurityObject = true;
        int id = 0;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (LwM2MBootstrapServerCredential serverCredential : serverConfiguration) {
            BootstrapConfig.ServerConfig serverConfig = setServerConfig((AbstractLwM2MBootstrapServerCredential) serverCredential);
            configBs.servers.put(id, serverConfig);
            BootstrapConfig.ServerSecurity serverSecurity = setServerSecurity((AbstractLwM2MBootstrapServerCredential) serverCredential, serverCredential.getSecurityMode());
            configBs.security.put(id, serverSecurity);
            id++;
        }
        /** in LwM2mDefaultBootstrapSessionManager -> initTasks
         * Delete all security/config objects if update bootstrap server and lwm2m server
         * if other: del or update only instances */

        return configBs;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setServerSecurity` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private BootstrapConfig.ServerSecurity setServerSecurity(AbstractLwM2MBootstrapServerCredential serverCredential, LwM2MSecurityMode securityMode) {
        BootstrapConfig.ServerSecurity serverSecurity = new BootstrapConfig.ServerSecurity();
        String serverUri = "coap://";
        byte[] publicKeyOrId = new byte[]{};
        byte[] secretKey = new byte[]{};
        byte[] serverPublicKey = new byte[]{};
        serverSecurity.serverId = serverCredential.getShortServerId();
        serverSecurity.securityMode = SecurityMode.valueOf(securityMode.name());
        serverSecurity.bootstrapServer = serverCredential.isBootstrapServerIs();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!LwM2MSecurityMode.NO_SEC.equals(securityMode)) {
            AbstractLwM2MBootstrapClientCredentialWithKeys server;
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (serverSecurity.bootstrapServer) {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.bootstrapServer;

            } else {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.lwm2mServer;
            }
            serverUri = "coaps://";
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (LwM2MSecurityMode.PSK.equals(securityMode)) {
                publicKeyOrId = server.getClientPublicKeyOrId().getBytes();
                secretKey = Hex.decodeHex(server.getClientSecretKey().toCharArray());
            } else {
                publicKeyOrId = server.getDecodedClientPublicKeyOrId();
                secretKey = server.getDecodedClientSecretKey();
            }
            serverPublicKey = serverCredential.getDecodedCServerPublicKey();
        }
        serverUri += (((serverCredential.getHost().equals("0.0.0.0") ? "localhost" : serverCredential.getHost()) + ":" + serverCredential.getPort()));
        serverSecurity.uri = serverUri;
        serverSecurity.publicKeyOrId = publicKeyOrId;
        serverSecurity.secretKey = secretKey;
        serverSecurity.serverPublicKey = serverPublicKey;
        return serverSecurity;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setServerConfig` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private BootstrapConfig.ServerConfig setServerConfig (AbstractLwM2MBootstrapServerCredential serverCredential) {
        BootstrapConfig.ServerConfig serverConfig = new BootstrapConfig.ServerConfig();
        serverConfig.shortId = serverCredential.getShortServerId();
        serverConfig.lifetime = serverCredential.getLifetime();
        serverConfig.defaultMinPeriod = serverCredential.getDefaultMinPeriod();
        serverConfig.notifIfDisabled = serverCredential.isNotifIfDisabled();
        serverConfig.binding = BindingMode.parse(serverCredential.getBinding());
        return serverConfig;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MBootstrapConfig` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

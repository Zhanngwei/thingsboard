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

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.eclipse.californium.core.config.CoapConfig.DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MNetworkConfig` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2MNetworkConfig {

    /**
     * 方法说明：
     * 1. 职责：执行 `getCoapConfig` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Configuration getCoapConfig(Integer serverPortNoSec, Integer serverSecurePort, LwM2MTransportServerConfig config) {
        Configuration coapConfig = new Configuration();
        coapConfig.set(CoapConfig.COAP_PORT, serverPortNoSec);
        coapConfig.set(CoapConfig.COAP_SECURE_PORT, serverSecurePort);
        /**
         Property to indicate if the response should always include the Block2 option \
         when client request early blockwise negociation but the response can be sent on one packet.
         - value of false indicate that the server will respond without block2 option if no further blocks are required.
         - value of true indicate that the server will response with block2 option event if no further blocks are required.
         CoAP client will try to use block mode
         or adapt the block size when receiving a 4.13 Entity too large response code
         */
        coapConfig.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, true);
        /**
         Property to indicate if the response should always include the Block2 option \
         when client request early blockwise negociation but the response can be sent on one packet.
         - value of false indicate that the server will respond without block2 option if no further blocks are required.
         - value of true indicate that the server will response with block2 option event if no further blocks are required.
         */
        coapConfig.set(CoapConfig.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, true);
        /**
         * The maximum amount of time (in milliseconds) allowed between
         * transfers of individual blocks in a blockwise transfer before the
         * blockwise transfer state is discarded.
         * <p>
         * The default value of this property is
         * {@link NetworkConfigDefaults#DEFAULT_BLOCKWISE_STATUS_LIFETIME} = 5 * 60 * 1000; // 5 mins [ms].
         */
        coapConfig.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS, TimeUnit.SECONDS);
        /**
         !!! REQUEST_ENTITY_TOO_LARGE CODE=4.13
         The maximum size of a resource body (in bytes) that will be accepted
         as the payload of a POST/PUT or the response to a GET request in a
         transparent> blockwise transfer.
         This option serves as a safeguard against excessive memory
         consumption when many resources contain large bodies that cannot be
         transferred in a single CoAP message. This option has no impact on
         *manually* managed blockwise transfers in which the blocks are handled individually.
         Note that this option does not prevent local clients or resource
         implementations from sending large bodies as part of a request or response to a peer.
         The default value of this property is DEFAULT_MAX_RESOURCE_BODY_SIZE = 8192
         A value of {@code 0} turns off transparent handling of blockwise transfers altogether.
         */
        coapConfig.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 256 * 1024 * 1024);
        /**
         The default DTLS response matcher.
         Supported values are STRICT, RELAXED, or PRINCIPAL.
         The default value is STRICT.
         Create new instance of udp endpoint context matcher.
         Params:
         checkAddress
         – true with address check, (STRICT, UDP) - if port Registration of client is changed - it is bad
         - false, without
         */
        coapConfig.set(CoapConfig.RESPONSE_MATCHING, CoapConfig.MatcherMode.RELAXED);
        /**
         https://tools.ietf.org/html/rfc7959#section-2.9.3
         The block size (number of bytes) to use when doing a blockwise transfer. \
         This value serves as the upper limit for block size in blockwise transfers
         */
        coapConfig.set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
        /**
         The maximum payload size (in bytes) that can be transferred in a
         single message, i.e. without requiring a blockwise transfer.
         NB: this value MUST be adapted to the maximum message size supported by the transport layer.
         In particular, this value cannot exceed the network's MTU if UDP is used as the transport protocol
         DEFAULT_VALUE = 1024
         */
        coapConfig.set(CoapConfig.MAX_MESSAGE_SIZE, 1024);

        coapConfig.set(CoapConfig.MAX_RETRANSMIT, 10);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!CollectionUtils.isEmpty(config.getNetworkConfig())) {
            Properties networkProps = new Properties();
            config.getNetworkConfig().forEach(p -> networkProps.put(p.getKey(), p.getValue()));
            coapConfig.add(networkProps);
        }

        return coapConfig;
    }

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MNetworkConfig` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}
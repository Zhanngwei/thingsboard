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
package org.thingsboard.server.transport.mqtt.util;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`ReturnCodeResolver` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class ReturnCodeResolver {

    /**
     * 方法说明：
     * 1. 职责：执行 `getConnectionReturnCode` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static MqttConnectReturnCode getConnectionReturnCode(MqttVersion mqttVersion, ReturnCode returnCode) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !ReturnCode.SUCCESS.equals(returnCode)) {
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (returnCode) {
                case BAD_USERNAME_OR_PASSWORD:
                case NOT_AUTHORIZED_5:
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    return MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
                case SERVER_UNAVAILABLE_5:
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    return MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
                case CLIENT_IDENTIFIER_NOT_VALID:
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    return MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
                default:
                    log.warn("Unknown return code for conversion: {}", returnCode.name());
            }
        }
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        return MqttConnectReturnCode.valueOf(returnCode.byteValue());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getSubscriptionReturnCode` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static int getSubscriptionReturnCode(MqttVersion mqttVersion, ReturnCode returnCode) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !ReturnCode.SUCCESS.equals(returnCode)) {
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (returnCode) {
                case UNSPECIFIED_ERROR:
                case TOPIC_FILTER_INVALID:
                case IMPLEMENTATION_SPECIFIC:
                case NOT_AUTHORIZED_5:
                case PACKET_IDENTIFIER_IN_USE:
                case QUOTA_EXCEEDED:
                case SHARED_SUBSCRIPTION_NOT_SUPPORTED:
                case SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED:
                case WILDCARD_SUBSCRIPTION_NOT_SUPPORTED:
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    return MqttQoS.FAILURE.value();
            }
        }
        return returnCode.byteValue();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ReturnCodeResolver` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

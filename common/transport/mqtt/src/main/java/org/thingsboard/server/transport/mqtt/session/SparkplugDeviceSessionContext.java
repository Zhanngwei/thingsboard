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
package org.thingsboard.server.transport.mqtt.session;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcRequestHeader;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProto;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`SparkplugDeviceSessionContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SparkplugDeviceSessionContext extends AbstractGatewayDeviceSessionContext<SparkplugNodeSessionHandler> {

    private final Map<String, SparkplugBProto.Payload.Metric> deviceBirthMetrics = new ConcurrentHashMap<>();

    /**
     * 方法说明：
     * 1. 职责：执行 `SparkplugDeviceSessionContext` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SparkplugDeviceSessionContext(SparkplugNodeSessionHandler parent,
                                         TransportDeviceInfo deviceInfo,
                                         DeviceProfile deviceProfile,
                                         ConcurrentMap<MqttTopicMatcher,
                                                 Integer> mqttQoSMap,
                                         TransportService transportService) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceBirthMetrics` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public  Map<String, SparkplugBProto.Payload.Metric> getDeviceBirthMetrics() {
        return deviceBirthMetrics;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setDeviceBirthMetrics` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setDeviceBirthMetrics(java.util.List<org.thingsboard.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics) {
        this.deviceBirthMetrics.putAll(metrics.stream()
                .collect(Collectors.toMap(SparkplugBProto.Payload.Metric::getName, metric -> metric)));
    }


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributeUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to sparkplug device", sessionId);
        notification.getSharedUpdatedList().forEach(tsKvProto -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        SparkplugMessageType.DCMD, deviceInfo.getDeviceName());
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(this.parent::writeAndFlush);
            }
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onToDeviceRpcRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}] Received RPC Request notification to sparkplug device", sessionId);
        try {
            SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
            SparkplugRpcRequestHeader header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
            header.setMessageType(messageType.name());
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        messageType, deviceInfo.getDeviceName());
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(payload -> parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo));
            } else {
                parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                                rpcRequest.getMethodName() + ". This device does not have a metricName: [" + tsKvProto.getKv().getKey() + "]");
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (ThingsboardException e) {
            parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                            rpcRequest.getMethodName() + ". " + e.getMessage());
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SparkplugDeviceSessionContext` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

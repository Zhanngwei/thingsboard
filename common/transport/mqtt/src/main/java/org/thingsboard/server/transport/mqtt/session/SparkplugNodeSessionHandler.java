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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.Descriptors;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.adaptor.ProtoConverter;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.ONLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.fromSparkplugBMetricToKeyValueProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.validatedValueByTypeMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.parseTopicSubscribe;

/**
 * Created by nickAS21 on 12.12.22
 */
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`SparkplugNodeSessionHandler` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SparkplugNodeSessionHandler extends AbstractGatewaySessionHandler<SparkplugDeviceSessionContext> {

    /**
     * 字段说明：
     * 1. 保存 `sparkplugTopicNode` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final SparkplugTopic sparkplugTopicNode;
    private final Map<String, SparkplugBProto.Payload.Metric> nodeBirthMetrics;
    /**
     * 字段说明：
     * 1. 保存 `parent` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final MqttTransportHandler parent;

    /**
     * 方法说明：
     * 1. 职责：执行 `SparkplugNodeSessionHandler` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SparkplugNodeSessionHandler(MqttTransportHandler parent, DeviceSessionCtx deviceSessionCtx, UUID sessionId,
                                       SparkplugTopic sparkplugTopicNode) {
        super(deviceSessionCtx, sessionId);
        this.parent = parent;
        this.sparkplugTopicNode = sparkplugTopicNode;
        this.nodeBirthMetrics = new ConcurrentHashMap<>();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setNodeBirthMetrics` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setNodeBirthMetrics(java.util.List<org.thingsboard.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics) {
        this.nodeBirthMetrics.putAll(metrics.stream()
                .collect(Collectors.toMap(SparkplugBProto.Payload.Metric::getName, metric -> metric)));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getNodeBirthMetrics` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Map<String, SparkplugBProto.Payload.Metric> getNodeBirthMetrics() {
        return this.nodeBirthMetrics;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertToPostTelemetry` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        byte[] bytes = getBytes(inbound.payload());
        Descriptors.Descriptor telemetryDynamicMsgDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getTelemetryDynamicMsgDescriptor());
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, telemetryDynamicMsgDescriptor)));
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.debug("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributesTelemetryProto` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAttributesTelemetryProto(int msgId, SparkplugBProto.Payload sparkplugBProto, SparkplugTopic topic) throws AdaptorException, ThingsboardException {
        String deviceName = topic.getNodeDeviceName();
        checkDeviceName(deviceName);

        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (topic.isNode()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (topic.isType(NBIRTH)) {
                sendSparkplugStateOnTelemetry(this.deviceSessionCtx.getSessionInfo(), deviceName, ONLINE,
                        sparkplugBProto.getTimestamp());
                setNodeBirthMetrics(sparkplugBProto.getMetricsList());
            }
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            contextListenableFuture = Futures.immediateFuture(this.deviceSessionCtx);
        } else {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            ListenableFuture<SparkplugDeviceSessionContext> deviceCtx = onDeviceConnectProto(topic);
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            contextListenableFuture = Futures.transform(deviceCtx, ctx -> {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (topic.isType(DBIRTH)) {
                    sendSparkplugStateOnTelemetry(ctx.getSessionInfo(), deviceName, ONLINE,
                            sparkplugBProto.getTimestamp());
                    ctx.setDeviceBirthMetrics(sparkplugBProto.getMetricsList());
                }
                return ctx;
            }, MoreExecutors.directExecutor());
        }
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        Set<String> attributesMetricNames = ((MqttDeviceProfileTransportConfiguration) deviceSessionCtx
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                .getDeviceProfile().getProfileData().getTransportConfiguration()).getSparkplugAttributesMetricNames();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (attributesMetricNames != null) {
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            List<TransportApiProtos.AttributesMsg> attributesMsgList = convertToPostAttributes(sparkplugBProto, attributesMetricNames, deviceName);
            onDeviceAttributesProto(contextListenableFuture, msgId, attributesMsgList, deviceName);
        }
        List<TransportProtos.PostTelemetryMsg> postTelemetryMsgList = convertToPostTelemetry(sparkplugBProto, attributesMetricNames, topic.getType().name());
        onDeviceTelemetryProto(contextListenableFuture, msgId, postTelemetryMsgList, deviceName);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceTelemetryProto` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceTelemetryProto(ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture,
                                       int msgId, List<TransportProtos.PostTelemetryMsg> postTelemetryMsgList, String deviceName) throws AdaptorException {
        try {
            int finalMsgId = msgId;
            postTelemetryMsgList.forEach(telemetryMsg -> {
                Futures.addCallback(contextListenableFuture,
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable MqttDeviceAwareSessionContext deviceCtx) {
                                try {
                                    processPostTelemetryMsg(deviceCtx, telemetryMsg, deviceName, finalMsgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}] Failed to convert telemetry: {}", gateway.getDeviceId(), deviceName, telemetryMsg, e);
                                    channel.close();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}] Failed to process device telemetry command: {}", sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            });
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceAttributesProto` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onDeviceAttributesProto(ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture, int msgId,
                                         List<TransportApiProtos.AttributesMsg> attributesMsgList, String deviceName) throws AdaptorException {
        try {
            if (!CollectionUtils.isEmpty(attributesMsgList)) {
                attributesMsgList.forEach(attributesMsg -> {
                    Futures.addCallback(contextListenableFuture,
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable MqttDeviceAwareSessionContext deviceCtx) {
                                    TransportProtos.PostAttributeMsg kvListProto = attributesMsg.getMsg();
                                    try {
                                        TransportProtos.PostAttributeMsg postAttributeMsg = ProtoConverter.validatePostAttributeMsg(kvListProto);
                                        processPostAttributesMsg(deviceCtx, postAttributeMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}] Failed to process device attributes command: {}", gateway.getDeviceId(), deviceName, kvListProto, e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}] Failed to process device attributes command: {}", sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}] Devices attributes keys list is empty for: [{}]", sessionId, gateway.getDeviceId());
            }
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleSparkplugSubscribeMsg` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleSparkplugSubscribeMsg(List<Integer> grantedQoSList, MqttTopicSubscription subscription,
                                            MqttQoS reqQoS) throws ThingsboardException, AdaptorException,
            ExecutionException, InterruptedException {
        SparkplugTopic sparkplugTopic = parseTopicSubscribe(subscription.topicName());
        if (sparkplugTopic.getGroupId() == null) {
            // TODO SUBSCRIBE NameSpace
        } else if (sparkplugTopic.getType() == null) {
            // TODO SUBSCRIBE GroupId
        } else if (sparkplugTopic.isNode()) {
            // SUBSCRIBE Node
            parent.processAttributesRpcSubscribeSparkplugNode(grantedQoSList, reqQoS);
        } else {
            // SUBSCRIBE Device - DO NOTHING, WE HAVE ALREADY SUBSCRIBED.
            // TODO: track that node subscribed to # or to particular device.
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceDisconnect` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceDisconnect(MqttPublishMessage mqttMsg, String deviceName) throws AdaptorException {
        try {
            processOnDisconnect(mqttMsg, deviceName);
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceConnectProto` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<SparkplugDeviceSessionContext> onDeviceConnectProto(SparkplugTopic topic) throws ThingsboardException {
        try {
            String deviceType = this.gateway.getDeviceType() + " device";
            return onDeviceConnect(topic.getNodeDeviceName(), deviceType);
        } catch (RuntimeException e) {
            log.error("Failed Sparkplug Device connect proto!", e);
            throw new ThingsboardException(e, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertToPostTelemetry` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<TransportProtos.PostTelemetryMsg> convertToPostTelemetry(SparkplugBProto.Payload sparkplugBProto, Set<String> attributesMetricNames, String topicTypeName) throws AdaptorException {
        try {
            List<TransportProtos.PostTelemetryMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                if (attributesMetricNames == null || !matches(attributesMetricNames, protoMetric)) {
                    long ts = protoMetric.getTimestamp();
                    String key = "bdSeq".equals(protoMetric.getName()) ?
                            topicTypeName + " " + protoMetric.getName() : protoMetric.getName();
                    Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = fromSparkplugBMetricToKeyValueProto(key, protoMetric);
                    if (keyValueProtoOpt.isPresent()) {
                        msgs.add(postTelemetryMsgCreated(keyValueProtoOpt.get(), ts));
                    }
                }
            }

            if (DBIRTH.name().equals(topicTypeName)) {
                TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
                keyValueProtoBuilder.setKey(topicTypeName + " " + "seq");
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                keyValueProtoBuilder.setLongV(sparkplugBProto.getSeq());
                msgs.add(postTelemetryMsgCreated(keyValueProtoBuilder.build(), sparkplugBProto.getTimestamp()));
            }
            return msgs;
        } catch (IllegalStateException | JsonSyntaxException | ThingsboardException e) {
            log.error("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertToPostAttributes` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<TransportApiProtos.AttributesMsg> convertToPostAttributes(SparkplugBProto.Payload sparkplugBProto,
                                                                           Set<String> attributesMetricNames,
                                                                           String deviceName) throws AdaptorException {
        try {
            List<TransportApiProtos.AttributesMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                if (matches(attributesMetricNames, protoMetric)) {
                    TransportApiProtos.AttributesMsg.Builder deviceAttributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
                    Optional<TransportProtos.PostAttributeMsg> msgOpt = getPostAttributeMsg(protoMetric);
                    if (msgOpt.isPresent()) {
                        deviceAttributesMsgBuilder.setDeviceName(deviceName);
                        deviceAttributesMsgBuilder.setMsg(msgOpt.get());
                        msgs.add(deviceAttributesMsgBuilder.build());
                    }
                }
            }
            return msgs;
        } catch (IllegalStateException | JsonSyntaxException | ThingsboardException e) {
            log.error("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `matches` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean matches(Set<String> attributesMetricNames, SparkplugBProto.Payload.Metric protoMetric) {
        String metricName = protoMetric.getName();
        for (String attributeMetricFilter : attributesMetricNames) {
            if (metricName.equals(attributeMetricFilter) ||
                    (attributeMetricFilter.endsWith("*") && metricName.startsWith(
                            attributeMetricFilter.substring(0, attributeMetricFilter.length() - 1)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getPostAttributeMsg` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Optional<TransportProtos.PostAttributeMsg> getPostAttributeMsg(SparkplugBProto.Payload.Metric protoMetric) throws ThingsboardException {
        Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = fromSparkplugBMetricToKeyValueProto(protoMetric.getName(), protoMetric);
        if (keyValueProtoOpt.isPresent()) {
            TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
            builder.addKv(keyValueProtoOpt.get());
            builder.setShared(true);
            return Optional.of(builder.build());
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getSparkplugTopicNode` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SparkplugTopic getSparkplugTopicNode() {
        return this.sparkplugTopicNode;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createSparkplugMqttPublishMsg` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Optional<MqttPublishMessage> createSparkplugMqttPublishMsg(TransportProtos.TsKvProto tsKvProto,
                                                                      String sparkplugTopic,
                                                                      SparkplugBProto.Payload.Metric metricBirth) {
        try {
            long ts = tsKvProto.getTs();
            MetricDataType metricDataType = MetricDataType.fromInteger(metricBirth.getDatatype());
            Optional value = validatedValueByTypeMetric(tsKvProto.getKv(), metricDataType);
            if (value.isPresent()) {
                SparkplugBProto.Payload.Builder cmdPayload = SparkplugBProto.Payload.newBuilder()
                        .setTimestamp(ts);
                cmdPayload.addMetrics(createMetric(value.get(), ts, tsKvProto.getKv().getKey(), metricDataType));
                byte[] payloadInBytes = cmdPayload.build().toByteArray();
                return Optional.of(getPayloadAdaptor().createMqttPublishMsg(deviceSessionCtx, sparkplugTopic, payloadInBytes));
            } else {
                log.trace("DeviceId: [{}] tenantId: [{}] sessionId:[{}] Failed to convert device attributes [{}] response to MQTT sparkplug  msg",
                        deviceSessionCtx.getDeviceInfo().getDeviceId(), deviceSessionCtx.getDeviceInfo().getTenantId(), sessionId, tsKvProto.getKv());
            }
        } catch (Exception e) {
            log.trace("DeviceId: [{}] tenantId: [{}] sessionId:[{}] Failed to convert device attributes response to MQTT sparkplug  msg",
                    deviceSessionCtx.getDeviceInfo().getDeviceId(), deviceSessionCtx.getDeviceInfo().getTenantId(), sessionId, e);
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `newDeviceSessionCtx` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected SparkplugDeviceSessionContext newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg) {
        return new SparkplugDeviceSessionContext(this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap, transportService);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendToDeviceRpcRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void sendToDeviceRpcRequest(MqttMessage payload, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, TransportProtos.SessionInfoProto sessionInfo) {
        parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendErrorRpcResponse` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ThingsboardErrorCode result, String errorMsg) {
        parent.sendErrorRpcResponse(sessionInfo, requestId, result, errorMsg);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendSuccessRpcResponse` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void sendSuccessRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ResponseCode result, String successMsg) {
        parent.sendSuccessRpcResponse(sessionInfo, requestId, result, successMsg);
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`SparkplugNodeSessionHandler` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

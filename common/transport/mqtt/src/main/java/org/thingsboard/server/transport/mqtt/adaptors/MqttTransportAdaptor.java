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
package org.thingsboard.server.transport.mqtt.adaptors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`MqttTransportAdaptor` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface MqttTransportAdaptor {

    ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    /**
     * 功能：转换遥测。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    /**
     * 功能：转换`To Get Attributes`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * - `topicBase`：主题名称或主题对象。
     * 返回：处理结果。
     */
    GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound, String topicBase) throws AdaptorException;

    /**
     * 功能：转换设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * - `topicBase`：主题名称或主题对象。
     * 返回：处理结果。
     */
    ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException;

    /**
     * 功能：转换RPC。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * - `topicBase`：主题名称或主题对象。
     * 返回：处理结果。
     */
    ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException;

    /**
     * 功能：转换设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    ClaimDeviceMsg convertToClaimDevice(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `responseMsg`：响应对象。
     * - `topicBase`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, GetAttributeResponseMsg responseMsg, String topicBase) throws AdaptorException;

    /**
     * 功能：转换`To Gateway Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceName`：设备信息或设备标识。
     * - `responseMsg`：响应对象。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, GetAttributeResponseMsg responseMsg) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationMsg`：待处理消息。
     * - `topic`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, AttributeUpdateNotificationMsg notificationMsg, String topic) throws AdaptorException;

    /**
     * 功能：转换`To Gateway Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceName`：设备信息或设备标识。
     * - `notificationMsg`：待处理消息。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `rpcRequest`：请求对象。
     * - `topicBase`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ToDeviceRpcRequestMsg rpcRequest, String topicBase) throws AdaptorException;

    /**
     * 功能：转换`To Gateway Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceName`：设备信息或设备标识。
     * - `rpcRequest`：请求对象。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `rpcResponse`：响应对象。
     * - `topicBase`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ToServerRpcResponseMsg rpcResponse, String topicBase) throws AdaptorException;

    /**
     * 功能：转换消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    ProvisionDeviceRequestMsg convertToProvisionRequestMsg(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `provisionResponse`：响应对象。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ProvisionDeviceResponseMsg provisionResponse) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `firmwareChunk`：`firmwareChunk` 参数。
     * - `requestId`：请求ID。
     * - `chunk`：`chunk` 参数。
     * - 其余参数：补充处理条件。
     * 返回：可能存在的结果。
     */
    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, byte[] firmwareChunk, String requestId, int chunk, OtaPackageType firmwareType) throws AdaptorException;

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `topic`：主题名称或主题对象。
     * - `payloadInBytes`：`payloadInBytes` 参数。
     * 返回：处理结果。
     */
    default MqttPublishMessage createMqttPublishMsg(MqttDeviceAwareSessionContext ctx, String topic, byte[] payloadInBytes) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, ctx.getQoSForTopic(topic), false, 0);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, ctx.nextMsgId());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(payloadInBytes);
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttTransportAdaptor` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

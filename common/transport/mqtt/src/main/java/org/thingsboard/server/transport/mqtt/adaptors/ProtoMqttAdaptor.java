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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.adaptor.ProtoConverter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.util.Optional;

import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_SOFTWARE_FIRMWARE_RESPONSES_TOPIC_FORMAT;

/**
 * 中文说明：
 * 1. 类目的：`ProtoMqttAdaptor` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@Slf4j
public class ProtoMqttAdaptor implements MqttTransportAdaptor {

    /**
     * 功能：转换遥测。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        byte[] bytes = toBytes(inbound.payload());
        Descriptors.Descriptor telemetryDynamicMsgDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getTelemetryDynamicMsgDescriptor());
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, telemetryDynamicMsgDescriptor)));
        } catch (Exception e) {
            log.debug("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        byte[] bytes = toBytes(inbound.payload());
        Descriptors.Descriptor attributesDynamicMessageDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getAttributesDynamicMessageDescriptor());
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, attributesDynamicMessageDescriptor)));
        } catch (Exception e) {
            log.debug("Failed to decode post attributes request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：转换设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        byte[] bytes = toBytes(inbound.payload());
        try {
            return ProtoConverter.convertToClaimDeviceProto(ctx.getDeviceId(), bytes);
        } catch (InvalidProtocolBufferException e) {
            log.debug("Failed to decode claim device request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：转换`To Get Attributes`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * - `topicBase`：主题名称或主题对象。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound, String topicBase) throws AdaptorException {
        byte[] bytes = toBytes(inbound.payload());
        String topicName = inbound.variableHeader().topicName();
        try {
            int requestId = getRequestId(topicName, topicBase);
            return ProtoConverter.convertToGetAttributeRequestMessage(bytes, requestId);
        } catch (InvalidProtocolBufferException e) {
            log.debug("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：转换设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * - `topicBase`：主题名称或主题对象。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        String topicName = mqttMsg.variableHeader().topicName();
        byte[] bytes = toBytes(mqttMsg.payload());
        Descriptors.Descriptor rpcResponseDynamicMessageDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getRpcResponseDynamicMessageDescriptor());
        try {
            int requestId = getRequestId(topicName, topicBase);
            JsonElement response = new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, rpcResponseDynamicMessageDescriptor));
            return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(response.toString()).build();
        } catch (Exception e) {
            log.debug("Failed to decode rpc response", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：转换RPC。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * - `topicBase`：主题名称或主题对象。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException {
        byte[] bytes = toBytes(mqttMsg.payload());
        String topicName = mqttMsg.variableHeader().topicName();
        try {
            int requestId = getRequestId(topicName, topicBase);
            return ProtoConverter.convertToServerRpcRequest(bytes, requestId);
        } catch (InvalidProtocolBufferException e) {
            log.debug("Failed to decode to server rpc request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg) throws AdaptorException {
        byte[] bytes = toBytes(mqttMsg.payload());
        try {
            return ProtoConverter.convertToProvisionRequestMsg(bytes);
        } catch (InvalidProtocolBufferException ex) {
            log.debug("Failed to decode provision request", ex);
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `responseMsg`：响应对象。
     * - `topicBase`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.GetAttributeResponseMsg responseMsg, String topicBase) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            int requestId = responseMsg.getRequestId();
            if (requestId >= 0) {
                return Optional.of(createMqttPublishMsg(ctx, topicBase + requestId, responseMsg.toByteArray()));
            }
            return Optional.empty();
        }
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `rpcRequest`：请求对象。
     * - `topicBase`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, String topicBase) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        DynamicMessage.Builder rpcRequestDynamicMessageBuilder = deviceSessionCtx.getRpcRequestDynamicMessageBuilder();
        if (rpcRequestDynamicMessageBuilder == null) {
            throw new AdaptorException("Failed to get rpcRequestDynamicMessageBuilder!");
        } else {
            return Optional.of(createMqttPublishMsg(ctx, topicBase + rpcRequest.getRequestId(), ProtoConverter.convertToRpcRequest(rpcRequest, rpcRequestDynamicMessageBuilder)));
        }
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `rpcResponse`：响应对象。
     * - `topicBase`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToServerRpcResponseMsg rpcResponse, String topicBase) {
        return Optional.of(createMqttPublishMsg(ctx, topicBase + rpcResponse.getRequestId(), rpcResponse.toByteArray()));
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `notificationMsg`：待处理消息。
     * - `topic`：主题名称或主题对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.AttributeUpdateNotificationMsg notificationMsg, String topic) {
        return Optional.of(createMqttPublishMsg(ctx, topic, notificationMsg.toByteArray()));
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `provisionResponse`：响应对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ProvisionDeviceResponseMsg provisionResponse) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC, provisionResponse.toByteArray()));
    }

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
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, byte[] firmwareChunk, String requestId, int chunk, OtaPackageType firmwareType) throws AdaptorException {
        return Optional.of(createMqttPublishMsg(ctx, String.format(DEVICE_SOFTWARE_FIRMWARE_RESPONSES_TOPIC_FORMAT, firmwareType.getKeyPrefix(), requestId, chunk), firmwareChunk));
    }

    /**
     * 功能：转换`To Gateway Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceName`：设备信息或设备标识。
     * - `responseMsg`：响应对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            TransportApiProtos.GatewayAttributeResponseMsg.Builder responseMsgBuilder = TransportApiProtos.GatewayAttributeResponseMsg.newBuilder();
            responseMsgBuilder.setDeviceName(deviceName);
            responseMsgBuilder.setResponseMsg(responseMsg);
            byte[] payloadBytes = responseMsgBuilder.build().toByteArray();
            return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, payloadBytes));
        }
    }

    /**
     * 功能：转换`To Gateway Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceName`：设备信息或设备标识。
     * - `notificationMsg`：待处理消息。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) {
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder builder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setNotificationMsg(notificationMsg);
        byte[] payloadBytes = builder.build().toByteArray();
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, payloadBytes));
    }

    /**
     * 功能：转换`To Gateway Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceName`：设备信息或设备标识。
     * - `rpcRequest`：请求对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        TransportApiProtos.GatewayDeviceRpcRequestMsg.Builder builder = TransportApiProtos.GatewayDeviceRpcRequestMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setRpcRequestMsg(rpcRequest);
        byte[] payloadBytes = builder.build().toByteArray();
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_RPC_TOPIC, payloadBytes));
    }

    /**
     * 功能：执行 `toBytes` 对应的处理。
     * 参数：
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    public static byte[] toBytes(ByteBuf inbound) {
        byte[] bytes = new byte[inbound.readableBytes()];
        int readerIndex = inbound.readerIndex();
        inbound.getBytes(readerIndex, bytes);
        return bytes;
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `topicName`：主题名称或主题对象。
     * - `topic`：主题名称或主题对象。
     * 返回：数值结果。
     */
    private int getRequestId(String topicName, String topic) {
        return Integer.parseInt(topicName.substring(topic.length()));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ProtoMqttAdaptor` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

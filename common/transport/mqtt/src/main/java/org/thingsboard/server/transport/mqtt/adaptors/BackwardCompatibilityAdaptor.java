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

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`BackwardCompatibilityAdaptor` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Data
@AllArgsConstructor
@Slf4j
public class BackwardCompatibilityAdaptor implements MqttTransportAdaptor {

    /**
     * Protobuf，表示当前对象的对应属性。
     */
    private MqttTransportAdaptor protoAdaptor;
    private MqttTransportAdaptor jsonAdaptor;

    /**
     * 功能：转换遥测。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        try {
            return protoAdaptor.convertToPostTelemetry(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process post telemetry request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToPostTelemetry(ctx, inbound);
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
        try {
            return protoAdaptor.convertToPostAttributes(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process post attributes request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToPostAttributes(ctx, inbound);
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
        try {
            return protoAdaptor.convertToGetAttributes(ctx, inbound, topicBase);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process get attributes request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToGetAttributes(ctx, inbound, topicBase);
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
        try {
            return protoAdaptor.convertToDeviceRpcResponse(ctx, mqttMsg, topicBase);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process to device rpc response msg: {} due to: ", ctx.getSessionId(), mqttMsg, e);
            return jsonAdaptor.convertToDeviceRpcResponse(ctx, mqttMsg, topicBase);
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
        try {
            return protoAdaptor.convertToServerRpcRequest(ctx, mqttMsg, topicBase);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process to server rpc request msg: {} due to: ", ctx.getSessionId(), mqttMsg, e);
            return jsonAdaptor.convertToServerRpcRequest(ctx, mqttMsg, topicBase);
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
        try {
            return protoAdaptor.convertToClaimDevice(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process claim device request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToClaimDevice(ctx, inbound);
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
        log.warn("[{}] invoked not implemented adaptor method! GetAttributeResponseMsg: {} TopicBase: {}", ctx.getSessionId(), responseMsg, topicBase);
        return Optional.empty();
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
        return protoAdaptor.convertToGatewayPublish(ctx, deviceName, responseMsg);
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
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.AttributeUpdateNotificationMsg notificationMsg, String topic) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! AttributeUpdateNotificationMsg: {} Topic: {}", ctx.getSessionId(), notificationMsg, topic);
        return Optional.empty();
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
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        return protoAdaptor.convertToGatewayPublish(ctx, deviceName, notificationMsg);
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
        log.warn("[{}] invoked not implemented adaptor method! ToDeviceRpcRequestMsg: {} TopicBase: {}", ctx.getSessionId(), rpcRequest, topicBase);
        return Optional.empty();
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
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return protoAdaptor.convertToGatewayPublish(ctx, deviceName, rpcRequest);
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
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ToServerRpcResponseMsg rpcResponse, String topicBase) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! ToServerRpcResponseMsg: {} TopicBase: {}", ctx.getSessionId(), rpcResponse, topicBase);
        return Optional.empty();
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! MqttPublishMessage: {}", ctx.getSessionId(), inbound);
        return null;
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `provisionResponse`：响应对象。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, TransportProtos.ProvisionDeviceResponseMsg provisionResponse) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! ProvisionDeviceResponseMsg: {}", ctx.getSessionId(), provisionResponse);
        return Optional.empty();
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
        return protoAdaptor.convertToPublish(ctx, firmwareChunk, requestId, chunk, firmwareType);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BackwardCompatibilityAdaptor` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

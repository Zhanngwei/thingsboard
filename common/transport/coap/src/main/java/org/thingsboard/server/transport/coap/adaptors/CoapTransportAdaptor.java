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
package org.thingsboard.server.transport.coap.adaptors;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`CoapTransportAdaptor` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface CoapTransportAdaptor {

    /**
     * 功能：转换遥测。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `telemetryMsgDescriptor`：待处理消息。
     * 返回：处理结果。
     */
    TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound, Descriptors.Descriptor telemetryMsgDescriptor) throws AdaptorException;

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `attributesMsgDescriptor`：待处理消息。
     * 返回：处理结果。
     */
    TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound, Descriptors.Descriptor attributesMsgDescriptor) throws AdaptorException;

    /**
     * 功能：转换`To Get Attributes`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    /**
     * 功能：转换设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `rpcResponseMsgDescriptor`：响应对象。
     * 返回：处理结果。
     */
    TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound, Descriptors.Descriptor rpcResponseMsgDescriptor) throws AdaptorException;

    /**
     * 功能：转换RPC。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException;

    /**
     * 功能：转换设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `sessionInfo`：会话对象。
     * 返回：处理结果。
     */
    TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `responseMsg`：响应对象。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `notificationMsg`：待处理消息。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `rpcRequest`：请求对象。
     * - `rpcRequestDynamicMessageBuilder`：请求对象。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.ToDeviceRpcRequestMsg rpcRequest, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException;

    /**
     * 功能：转换消息。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, Request inbound) throws AdaptorException;

    /**
     * 功能：获取内容格式。
     * 参数：无。
     * 返回：数值结果。
     */
    int getContentFormat();

}

/*
 * 本类总结：
 * 1. 核心职责：`CoapTransportAdaptor` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

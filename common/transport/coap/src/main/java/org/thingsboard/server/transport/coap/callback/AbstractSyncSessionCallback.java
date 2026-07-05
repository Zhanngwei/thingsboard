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
package org.thingsboard.server.transport.coap.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;
import org.thingsboard.server.transport.coap.client.TbCoapContentFormatUtil;
import org.thingsboard.server.transport.coap.client.TbCoapObservationState;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`AbstractSyncSessionCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractSyncSessionCallback implements SessionMsgListener {

    /**
     * 状态，表示当前对象所处状态。
     */
    protected final TbCoapClientState state;
    protected final CoapExchange exchange;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    protected final Request request;

    /**
     * 功能：处理响应。
     * 参数：
     * - `getAttributesResponse`：响应对象。
     * 返回：无。
     */
    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg getAttributesResponse) {
        logUnsupportedCommandMessage(getAttributesResponse);
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `sessionId`：会话ID。
     * - `attributeUpdateNotification`：`attributeUpdateNotification` 参数。
     * 返回：无。
     */
    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotification) {
        logUnsupportedCommandMessage(attributeUpdateNotification);
    }

    /**
     * 功能：处理会话。
     * 参数：
     * - `sessionId`：会话ID。
     * - `sessionCloseNotification`：会话对象。
     * 返回：无。
     */
    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {

    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    @Override
    public void onDeviceDeleted(DeviceId deviceId) {

    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `toDeviceRequest`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg toDeviceRequest) {
        logUnsupportedCommandMessage(toDeviceRequest);
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `toServerResponse`：响应对象。
     * 返回：无。
     */
    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        logUnsupportedCommandMessage(toServerResponse);
    }

    /**
     * 功能：执行 `logUnsupportedCommandMessage` 对应的处理。
     * 参数：
     * - `update`：`update` 参数。
     * 返回：无。
     */
    private void logUnsupportedCommandMessage(Object update) {
        log.trace("[{}] Ignore unsupported update: {}", state.getDeviceId(), update);
    }

    /**
     * 功能：判断请求。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：判断结果。
     */
    public static boolean isConRequest(TbCoapObservationState state) {
        if (state != null) {
            return state.getExchange().advanced().getRequest().isConfirmable();
        } else {
            return false;
        }
    }

    /**
     * 功能：执行 `respond` 对应的处理。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    protected void respond(Response response) {
        response.getOptions().setContentFormat(TbCoapContentFormatUtil.getContentFormat(exchange.getRequestOptions().getContentFormat(), state.getContentFormat()));
        response.setConfirmable(exchange.advanced().getRequest().isConfirmable());
        exchange.respond(response);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractSyncSessionCallback` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

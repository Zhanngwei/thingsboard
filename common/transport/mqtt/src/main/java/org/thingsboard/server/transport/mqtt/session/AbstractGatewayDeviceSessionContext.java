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

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ashvayka on 19.01.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractGatewayDeviceSessionContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public abstract class AbstractGatewayDeviceSessionContext<T extends AbstractGatewaySessionHandler> extends MqttDeviceAwareSessionContext implements SessionMsgListener {

    /**
     * `parent` 字段，保存当前对象的对应属性。
     */
    protected final T parent;
    private final TransportService transportService;

    /**
     * 功能：创建 `AbstractGatewayDeviceSessionContext` 实例，并初始化必要字段。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `deviceInfo`：设备信息或设备标识。
     * - `deviceProfile`：设备信息或设备标识。
     * - `mqttQoSMap`：键值映射。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AbstractGatewayDeviceSessionContext(T parent, TransportDeviceInfo deviceInfo,
                                               DeviceProfile deviceProfile, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap,
                                               TransportService transportService) {
        super(UUID.randomUUID(), mqttQoSMap);
        this.parent = parent;
        setSessionInfo(SessionInfoProto.newBuilder()
                .setNodeId(parent.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(deviceInfo.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceInfo.getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(deviceInfo.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(deviceInfo.getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(deviceInfo.getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(deviceInfo.getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(deviceInfo.getDeviceName())
                .setDeviceType(deviceInfo.getDeviceType())
                .setGwSessionIdMSB(parent.getSessionId().getMostSignificantBits())
                .setGwSessionIdLSB(parent.getSessionId().getLeastSignificantBits())
                .setDeviceProfileIdMSB(deviceInfo.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(deviceInfo.getDeviceProfileId().getId().getLeastSignificantBits())
                .build());
        setDeviceInfo(deviceInfo);
        setConnected(true);
        setDeviceProfile(deviceProfile);
        this.transportService = transportService;
    }

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * 功能：执行 `nextMsgId` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int nextMsgId() {
        return parent.nextMsgId();
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg response) {
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), response).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `sessionId`：会话ID。
     * - `notification`：`notification` 参数。
     * 返回：无。
     */
    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), notification).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `request`：请求对象。
     * 返回：无。
     */
    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg request) {
        log.trace("[{}] Received RPC command to device", sessionId);
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), request).ifPresent(
                    payload -> {
                        ChannelFuture channelFuture = parent.writeAndFlush(payload);
                        if (request.getPersisted()) {
                            channelFuture.addListener(result -> {
                                if (result.cause() == null) {
                                    if (!isAckExpected(payload)) {
                                        transportService.process(getSessionInfo(), request, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
                                    } else if (request.getPersisted()) {
                                        transportService.process(getSessionInfo(), request, RpcStatus.SENT, TransportServiceCallback.EMPTY);

                                    }
                                }
                            });
                        }
                    }
            );
        } catch (Exception e) {
            transportService.process(getSessionInfo(),
                    TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                            .setRequestId(request.getRequestId()).setError("Failed to convert device RPC command to MQTT msg").build(), TransportServiceCallback.EMPTY);
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
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
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        parent.deregisterSession(getDeviceInfo().getDeviceName());
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `toServerResponse`：响应对象。
     * 返回：无。
     */
    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        // This feature is not supported in the TB IoT Gateway yet.
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        parent.onDeviceDeleted(this.getSessionInfo().getDeviceName());
    }

    /**
     * 功能：判断`Ack Expected`。
     * 参数：
     * - `message`：待处理消息。
     * 返回：判断结果。
     */
    private boolean isAckExpected(MqttMessage message) {
        return message.fixedHeader().qosLevel().value() > 0;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractGatewayDeviceSessionContext` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

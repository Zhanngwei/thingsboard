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
package org.thingsboard.server.transport.snmp.session;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. 类目的：`DeviceSessionContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class DeviceSessionContext extends DeviceAwareSessionContext implements SessionMsgListener, ResponseListener {
    /**
     * 目标对象，表示当前对象的对应属性。
     */
    @Getter
    private Target target;
    private final String token;
    /**
     * 配置，保存当前对象的配置选项。
     */
    @Getter
    @Setter
    private SnmpDeviceProfileTransportConfiguration profileTransportConfiguration;
    /**
     * 设备，保存当前对象的配置选项。
     */
    @Getter
    @Setter
    private SnmpDeviceTransportConfiguration deviceTransportConfiguration;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Getter
    @Setter
    private Device device;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    private final TenantId tenantId;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final SnmpTransportContext snmpTransportContext;

    private final AtomicInteger msgIdSeq = new AtomicInteger(0);
    /**
     * 当前对象是否处于激活状态。
     */
    @Getter
    private boolean isActive = true;
    /**
     * 会话，负责处理对应任务或消息。
     */
    @Setter
    private Runnable sessionTimeoutHandler;

    @Getter
    private final List<ScheduledTask> queryingTasks = new LinkedList<>();

    /**
     * 功能：创建 `DeviceSessionContext` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `deviceProfile`：设备信息或设备标识。
     * - `token`：`token` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    public DeviceSessionContext(TenantId tenantId, Device device, DeviceProfile deviceProfile, String token,
                                SnmpDeviceProfileTransportConfiguration profileTransportConfiguration,
                                SnmpDeviceTransportConfiguration deviceTransportConfiguration,
                                SnmpTransportContext snmpTransportContext) throws Exception {
        super(UUID.randomUUID());
        super.setDeviceId(device.getId());
        super.setDeviceProfile(deviceProfile);
        this.device = device;
        this.tenantId = tenantId;

        this.token = token;
        this.snmpTransportContext = snmpTransportContext;

        this.profileTransportConfiguration = profileTransportConfiguration;
        this.deviceTransportConfiguration = deviceTransportConfiguration;

        initializeTarget(profileTransportConfiguration, deviceTransportConfiguration);
    }

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `newSessionInfo`：会话对象。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto newSessionInfo, DeviceProfile deviceProfile) {
        super.onDeviceProfileUpdate(newSessionInfo, deviceProfile);
        if (isActive) {
            snmpTransportContext.onDeviceProfileUpdated(deviceProfile, this);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        snmpTransportContext.onDeviceDeleted(this);
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @Override
    public void onResponse(ResponseEvent event) {
        if (isActive) {
            snmpTransportContext.getSnmpTransportService().processResponseEvent(this, event);
        }
    }

    /**
     * 功能：执行 `initializeTarget` 对应的处理。
     * 参数：
     * - `profileTransportConfig`：配置对象。
     * - `deviceTransportConfig`：设备信息或设备标识。
     * 返回：无。
     */
    public void initializeTarget(SnmpDeviceProfileTransportConfiguration profileTransportConfig, SnmpDeviceTransportConfiguration deviceTransportConfig) throws Exception {
        log.trace("Initializing target for SNMP session of device {}", device);
        this.target = snmpTransportContext.getSnmpAuthService().setUpSnmpTarget(profileTransportConfig, deviceTransportConfig);
        log.debug("SNMP target initialized: {}", target);
    }

    /**
     * 功能：执行 `close` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void close() {
        isActive = false;
    }

    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getToken() {
        return token;
    }

    /**
     * 功能：执行 `nextMsgId` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `getAttributesResponse`：响应对象。
     * 返回：无。
     */
    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse) {
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `sessionId`：会话ID。
     * - `attributeUpdateNotification`：`attributeUpdateNotification` 参数。
     * 返回：无。
     */
    @Override
    public void onAttributeUpdate(UUID sessionId, AttributeUpdateNotificationMsg attributeUpdateNotification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        try {
            snmpTransportContext.getSnmpTransportService().onAttributeUpdate(this, attributeUpdateNotification);
        } catch (Exception e) {
            snmpTransportContext.getTransportService().errorEvent(getTenantId(), getDeviceId(), SnmpCommunicationSpec.SHARED_ATTRIBUTES_SETTING.getLabel(), e);
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
    public void onRemoteSessionCloseCommand(UUID sessionId, SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        if (sessionCloseNotification.getMessage().equals(DefaultTransportService.SESSION_EXPIRED_MESSAGE)) {
            if (sessionTimeoutHandler != null) {
                sessionTimeoutHandler.run();
            }
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `toDeviceRequest`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onToDeviceRpcRequest(UUID sessionId, ToDeviceRpcRequestMsg toDeviceRequest) {
        log.trace("[{}] Received RPC command to device", sessionId);
        try {
            snmpTransportContext.getSnmpTransportService().onToDeviceRpcRequest(this, toDeviceRequest);
            snmpTransportContext.getTransportService().process(getSessionInfo(), toDeviceRequest, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
        } catch (Exception e) {
            snmpTransportContext.getTransportService().errorEvent(getTenantId(), getDeviceId(), SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST.getLabel(), e);
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `toServerResponse`：响应对象。
     * 返回：无。
     */
    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceSessionContext` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

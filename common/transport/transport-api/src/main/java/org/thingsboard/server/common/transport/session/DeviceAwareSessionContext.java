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
package org.thingsboard.server.common.transport.session;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`DeviceAwareSessionContext` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
@Data
public abstract class DeviceAwareSessionContext implements SessionContext {

    /**
     * 会话ID，用于定位对应业务对象。
     */
    @Getter
    protected final UUID sessionId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    @Getter
    private volatile DeviceId deviceId;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Getter
    protected volatile TransportDeviceInfo deviceInfo;
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @Getter
    @Setter
    protected volatile DeviceProfile deviceProfile;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Getter
    @Setter
    protected volatile TransportProtos.SessionInfoProto sessionInfo;

    /**
     * 当前连接是否已经建立。
     */
    @Setter
    private volatile boolean connected;

    /**
     * 功能：获取设备ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public DeviceId getDeviceId() {
        return deviceId;
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `deviceInfo`：设备信息或设备标识。
     * 返回：无。
     */
    public void setDeviceInfo(TransportDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        this.deviceId = deviceInfo.getDeviceId();
    }

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        this.sessionInfo = sessionInfo;
        this.deviceProfile = deviceProfile;
        this.deviceInfo.setDeviceType(deviceProfile.getName());

    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `device`：设备信息或设备标识。
     * - `deviceProfileOpt`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        this.sessionInfo = sessionInfo;
        this.deviceInfo.setDeviceProfileId(device.getDeviceProfileId());
        this.deviceInfo.setDeviceType(device.getType());
        deviceProfileOpt.ifPresent(profile -> this.deviceProfile = profile);
    }

    /**
     * 功能：判断`Connected`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * 功能：更新`Disconnected`。
     * 参数：无。
     * 返回：无。
     */
    public void setDisconnected() {
        this.connected = false;
    }

    /**
     * 功能：判断Sparkplug。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSparkplug() {
        DeviceProfileTransportConfiguration transportConfiguration = this.deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            return ((MqttDeviceProfileTransportConfiguration) transportConfiguration).isSparkplug();
        } else {
            return false;
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceAwareSessionContext` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

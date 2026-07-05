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
package org.thingsboard.server.transport.coap.client;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.TransportConfigurationContainer;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`TbCoapClientState` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Data
public class TbCoapClientState {

    /**
     * 设备ID，用于定位对应业务对象。
     */
    private final DeviceId deviceId;
    private final Lock lock;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    private volatile TransportConfigurationContainer configuration;
    private volatile CoapTransportAdaptor adaptor;
    /**
     * 凭据，用于认证或安全校验。
     */
    private volatile ValidateDeviceCredentialsResponse credentials;
    private volatile TransportProtos.SessionInfoProto session;
    /**
     * 监听器列表，用于保存一组待处理对象。
     */
    private volatile DefaultCoapClientContext.CoapSessionListener listener;
    private volatile TbCoapObservationState attrs;
    /**
     * RPC，表示当前对象的对应属性。
     */
    private volatile TbCoapObservationState rpc;
    private volatile int contentFormat;

    /**
     * 属性，表示当前对象的对应属性。
     */
    private TransportProtos.AttributeUpdateNotificationMsg missedAttributeUpdates;

    /**
     * 配置ID，用于定位对应业务对象。
     */
    private DeviceProfileId profileId;

    /**
     * `powerMode` 字段，保存当前对象的对应属性。
     */
    @Getter
    private PowerMode powerMode;
    /**
     * 定时器，用于安排延迟任务或周期任务。
     */
    @Getter
    private Long psmActivityTimer;
    /**
     * `edrxCycle` 字段，保存当前对象的对应属性。
     */
    @Getter
    private Long edrxCycle;
    /**
     * `pagingTransmissionWindow` 字段，保存当前对象的对应属性。
     */
    @Getter
    private Long pagingTransmissionWindow;
    /**
     * 是否满足`asleep`条件。
     */
    @Getter
    @Setter
    private boolean asleep;
    /**
     * 时间，用于控制时间范围或等待时长。
     */
    @Getter
    private long lastUplinkTime;
    /**
     * `sleepTask` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Setter
    private Future<Void> sleepTask;

    /**
     * 是否满足`firstEdrxDownlink`条件。
     */
    private boolean firstEdrxDownlink = true;

    /**
     * 功能：创建 `TbCoapClientState` 实例，并初始化必要字段。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：新创建的对象实例。
     */
    public TbCoapClientState(DeviceId deviceId) {
        this.deviceId = deviceId;
        this.lock = new ReentrantLock();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `credentials`：`credentials` 参数。
     * 返回：无。
     */
    public void init(ValidateDeviceCredentialsResponse credentials) {
        this.credentials = credentials;
        this.profileId = credentials.getDeviceInfo().getDeviceProfileId();
        this.powerMode = credentials.getDeviceInfo().getPowerMode();
        this.edrxCycle = credentials.getDeviceInfo().getEdrxCycle();
        this.psmActivityTimer = credentials.getDeviceInfo().getPsmActivityTimer();
        this.pagingTransmissionWindow = credentials.getDeviceInfo().getPagingTransmissionWindow();
    }

    /**
     * 功能：执行 `lock` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void lock() {
        lock.lock();
    }

    /**
     * 功能：执行 `unlock` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void unlock() {
        lock.unlock();
    }

    /**
     * 功能：更新时间。
     * 参数：
     * - `ts`：时间戳。
     * 返回：数值结果。
     */
    public long updateLastUplinkTime(long ts) {
        if (ts > lastUplinkTime) {
            this.lastUplinkTime = ts;
            this.firstEdrxDownlink = true;
        }
        return lastUplinkTime;
    }

    /**
     * 功能：校验`First Downlink`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean checkFirstDownlink() {
        boolean result = firstEdrxDownlink;
        firstEdrxDownlink = false;
        return result;
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：无。
     */
    public void onDeviceUpdate(Device device) {
        this.profileId = device.getDeviceProfileId();
        var data = device.getDeviceData();
        if (data.getTransportConfiguration() != null && data.getTransportConfiguration().getType().equals(DeviceTransportType.COAP)) {
            CoapDeviceTransportConfiguration configuration = (CoapDeviceTransportConfiguration) data.getTransportConfiguration();
            this.powerMode = configuration.getPowerMode();
            this.edrxCycle = configuration.getEdrxCycle();
            this.psmActivityTimer = configuration.getPsmActivityTimer();
            this.pagingTransmissionWindow = configuration.getPagingTransmissionWindow();
        }
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void addQueuedNotification(TransportProtos.AttributeUpdateNotificationMsg msg) {
        if (missedAttributeUpdates == null) {
            missedAttributeUpdates = msg;
        } else {
            Map<String, TransportProtos.TsKvProto> updatedAttrs = new HashMap<>(missedAttributeUpdates.getSharedUpdatedCount() + msg.getSharedUpdatedCount());
            Set<String> deletedKeys = new HashSet<>(missedAttributeUpdates.getSharedDeletedCount() + msg.getSharedDeletedCount());
            for (TransportProtos.TsKvProto oldUpdatedAttrs : missedAttributeUpdates.getSharedUpdatedList()) {
                updatedAttrs.put(oldUpdatedAttrs.getKv().getKey(), oldUpdatedAttrs);
            }
            deletedKeys.addAll(msg.getSharedDeletedList());
            for (TransportProtos.TsKvProto newUpdatedAttrs : msg.getSharedUpdatedList()) {
                updatedAttrs.put(newUpdatedAttrs.getKv().getKey(), newUpdatedAttrs);
            }
            deletedKeys.addAll(msg.getSharedDeletedList());
            for (String deletedKey : msg.getSharedDeletedList()) {
                updatedAttrs.remove(deletedKey);
            }
            missedAttributeUpdates = TransportProtos.AttributeUpdateNotificationMsg.newBuilder().addAllSharedUpdated(updatedAttrs.values()).addAllSharedDeleted(deletedKeys).build();
        }
    }

    /**
     * 功能：获取`And Clear Missed Updates`。
     * 参数：无。
     * 返回：处理结果。
     */
    public TransportProtos.AttributeUpdateNotificationMsg getAndClearMissedUpdates() {
        var result = this.missedAttributeUpdates;
        this.missedAttributeUpdates = null;
        return result;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbCoapClientState` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

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
 * 1. `TbCoapClientState` 是 ThingsBoard Common Transport 中承载 CoAP 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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

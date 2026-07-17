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
package org.thingsboard.server.service.state;

import org.springframework.context.ApplicationListener;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

/**
 * Created by ashvayka on 01.05.18.
 */
/**
 * 中文说明：
 * 1. `DeviceStateService` 是 ThingsBoard Application 中定义设备能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `ApplicationListener`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceStateService extends ApplicationListener<PartitionChangeEvent> {

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastConnectTime`：`lastConnectTime` 参数。
     * 返回：无。
     */
    void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long lastConnectTime);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    default void onDeviceConnect(TenantId tenantId, DeviceId deviceId) {
        onDeviceConnect(tenantId, deviceId, System.currentTimeMillis());
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastReportedActivityTime`：`lastReportedActivityTime` 参数。
     * 返回：无。
     */
    void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long lastReportedActivityTime);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastDisconnectTime`：`lastDisconnectTime` 参数。
     * 返回：无。
     */
    void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long lastDisconnectTime);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    default void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId) {
        onDeviceDisconnect(tenantId, deviceId, System.currentTimeMillis());
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastInactivityTime`：`lastInactivityTime` 参数。
     * 返回：无。
     */
    void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long lastInactivityTime);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `inactivityTimeout`：`inactivityTimeout` 参数。
     * 返回：无。
     */
    void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout);

    /**
     * 功能：处理队列。
     * 参数：
     * - `proto`：`proto` 参数。
     * - `bytes`：`bytes` 参数。
     * 返回：无。
     */
    void onQueueMsg(TransportProtos.DeviceStateServiceMsgProto proto, TbCallback bytes);

}

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
package org.thingsboard.server.cache.device;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `DeviceCacheKey` 是 ThingsBoard Common Cache 中管理设备缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class DeviceCacheKey implements Serializable {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final DeviceId deviceId;
    /**
     * 设备，用于标识或展示当前对象。
     */
    private final String deviceName;

    /**
     * 功能：创建 `DeviceCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：新创建的对象实例。
     */
    public DeviceCacheKey(DeviceId deviceId) {
        this(null, deviceId, null);
    }

    /**
     * 功能：创建 `DeviceCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：新创建的对象实例。
     */
    public DeviceCacheKey(TenantId tenantId, DeviceId deviceId) {
        this(tenantId, deviceId, null);
    }

    /**
     * 功能：创建 `DeviceCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceName`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public DeviceCacheKey(TenantId tenantId, String deviceName) {
        this(tenantId, null, deviceName);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        if (deviceId == null) {
            return tenantId + "_n_" + deviceName;
        } else if (tenantId == null) {
            return deviceId.toString();
        } else {
            return tenantId + "_" + deviceId;
        }
    }
}

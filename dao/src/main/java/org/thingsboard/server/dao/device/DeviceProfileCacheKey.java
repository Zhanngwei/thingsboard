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
package org.thingsboard.server.dao.device;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `DeviceProfileCacheKey` 是 ThingsBoard DAO 中管理设备配置缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Data
public class DeviceProfileCacheKey implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8220455917177676472L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final String name;
    /**
     * 设备配置ID，用于定位对应业务对象。
     */
    private final DeviceProfileId deviceProfileId;
    private final boolean defaultProfile;
    /**
     * 设备，用于定位映射、配置或数据项。
     */
    private final String provisionDeviceKey;

    /**
     * 功能：创建 `DeviceProfileCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `deviceProfileId`：设备配置ID。
     * - `defaultProfile`：`defaultProfile` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    private DeviceProfileCacheKey(TenantId tenantId, String name, DeviceProfileId deviceProfileId, boolean defaultProfile, String provisionDeviceKey) {
        this.tenantId = tenantId;
        this.name = name;
        this.deviceProfileId = deviceProfileId;
        this.defaultProfile = defaultProfile;
        this.provisionDeviceKey = provisionDeviceKey;
    }

    /**
     * 功能：执行 `fromName` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static DeviceProfileCacheKey fromName(TenantId tenantId, String name) {
        return new DeviceProfileCacheKey(tenantId, name, null, false, null);
    }

    /**
     * 功能：执行 `fromId` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    public static DeviceProfileCacheKey fromId(DeviceProfileId id) {
        return new DeviceProfileCacheKey(null, null, id, false, null);
    }

    /**
     * 功能：执行 `defaultProfile` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    public static DeviceProfileCacheKey defaultProfile(TenantId tenantId) {
        return new DeviceProfileCacheKey(tenantId, null, null, true, null);
    }

    /**
     * 功能：执行 `fromProvisionDeviceKey` 对应的处理。
     * 参数：
     * - `provisionDeviceKey`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public static DeviceProfileCacheKey fromProvisionDeviceKey(String provisionDeviceKey) {
        return new DeviceProfileCacheKey(null, null, null, false, provisionDeviceKey);
    }

    /**
     * IMPORTANT: Method toString() has to return unique value, if you add additional field to this class, please also refactor toString().
     */
    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        if (deviceProfileId != null) {
            return deviceProfileId.toString();
        } else if (defaultProfile) {
            return tenantId.toString();
        } else if (StringUtils.isNotEmpty(provisionDeviceKey)) {
            return provisionDeviceKey;
        }
        return tenantId + "_" + name;
    }
}

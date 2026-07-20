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
package org.thingsboard.server.dao.tenant;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantProfileId;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `TenantProfileCacheKey` 是 ThingsBoard DAO 中管理租户缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Data
public class TenantProfileCacheKey implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8220455917177676472L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantProfileId tenantProfileId;
    private final boolean defaultProfile;

    /**
     * 功能：创建 `TenantProfileCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * - `defaultProfile`：`defaultProfile` 参数。
     * 返回：新创建的对象实例。
     */
    private TenantProfileCacheKey(TenantProfileId tenantProfileId, boolean defaultProfile) {
        this.tenantProfileId = tenantProfileId;
        this.defaultProfile = defaultProfile;
    }

    /**
     * 功能：执行 `fromId` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    public static TenantProfileCacheKey fromId(TenantProfileId id) {
        return new TenantProfileCacheKey(id, false);
    }

    /**
     * 功能：执行 `defaultProfile` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static TenantProfileCacheKey defaultProfile() {
        return new TenantProfileCacheKey(null, true);
    }


    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        if (defaultProfile) {
            return "default";
        } else {
            return tenantProfileId.toString();
        }
    }
}

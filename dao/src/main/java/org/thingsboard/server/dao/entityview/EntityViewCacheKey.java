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
package org.thingsboard.server.dao.entityview;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `EntityViewCacheKey` 是 ThingsBoard DAO 中管理实体视图缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Getter
@EqualsAndHashCode
@Builder
public class EntityViewCacheKey implements Serializable {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final String name;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    private final EntityId entityId;
    private final EntityViewId entityViewId;

    /**
     * 功能：创建 `EntityViewCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `entityId`：实体IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：新创建的对象实例。
     */
    private EntityViewCacheKey(TenantId tenantId, String name, EntityId entityId, EntityViewId entityViewId) {
        this.tenantId = tenantId;
        this.name = name;
        this.entityId = entityId;
        this.entityViewId = entityViewId;
    }

    /**
     * 功能：执行 `byName` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static EntityViewCacheKey byName(TenantId tenantId, String name) {
        return new EntityViewCacheKey(tenantId, name, null, null);
    }

    /**
     * 功能：执行 `byEntityId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    public static EntityViewCacheKey byEntityId(TenantId tenantId, EntityId entityId) {
        return new EntityViewCacheKey(tenantId, null, entityId, null);
    }

    /**
     * 功能：执行 `byId` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    public static EntityViewCacheKey byId(EntityViewId id) {
        return new EntityViewCacheKey(null, null, null, id);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        if (entityViewId != null) {
            return entityViewId.toString();
        } else if (entityId != null) {
            return tenantId + "_" + entityId;
        } else {
            return tenantId + "_n_" + name;
        }
    }

}

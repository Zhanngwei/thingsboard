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
package org.thingsboard.server.dao.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `BaseEntityCountService` 是 ThingsBoard DAO 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCachedEntityService`、`EntityCountService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
public class BaseEntityCountService extends AbstractCachedEntityService<EntityCountCacheKey, Long, EntityCountCacheEvictEvent> implements EntityCountService {

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private EntityServiceRegistry entityServiceRegistry;

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * 返回：数值结果。
     */
    @Override
    public long countByTenantIdAndEntityType(TenantId tenantId, EntityType entityType) {
        return cache.getAndPutInTransaction(new EntityCountCacheKey(tenantId, entityType),
                () -> entityServiceRegistry.getServiceByEntityType(entityType).countByTenantId(tenantId), false);
    }

    /**
     * 功能：发送或提交实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    @Override
    public void publishCountEntityEvictEvent(TenantId tenantId, EntityType entityType) {
        publishEvictEvent(new EntityCountCacheEvictEvent(tenantId, entityType));
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = EntityCountCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(EntityCountCacheEvictEvent event) {
        cache.evict(new EntityCountCacheKey(event.getTenantId(), event.getEntityType()));
    }
}

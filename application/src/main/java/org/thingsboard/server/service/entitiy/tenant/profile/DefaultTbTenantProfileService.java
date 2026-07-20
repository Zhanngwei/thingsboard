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
package org.thingsboard.server.service.entitiy.tenant.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;

import java.util.List;

/**
 * 中文说明：
 * 1. `DefaultTbTenantProfileService` 是 ThingsBoard Application 中负责租户的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbTenantProfileService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbTenantProfileService extends AbstractTbEntityService implements TbTenantProfileService {
    /**
     * 队列，提供当前类调用的业务操作。
     */
    private final TbQueueService tbQueueService;
    private final TenantProfileService tenantProfileService;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;
    private final TbTenantProfileCache tenantProfileCache;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfile`：租户信息或租户标识。
     * - `oldTenantProfile`：租户信息或租户标识。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile save(TenantId tenantId, TenantProfile tenantProfile, TenantProfile oldTenantProfile) throws ThingsboardException {
        TenantProfile savedTenantProfile = checkNotNull(tenantProfileService.saveTenantProfile(tenantId, tenantProfile));
        tenantProfileCache.put(savedTenantProfile);
        tbClusterService.onTenantProfileChange(savedTenantProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, savedTenantProfile.getId(),
                tenantProfile.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

        List<TenantId> tenantIds = tenantService.findTenantIdsByTenantProfileId(savedTenantProfile.getId());
        tbQueueService.updateQueuesByTenants(tenantIds, savedTenantProfile, oldTenantProfile);

        return savedTenantProfile;
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：无。
     */
    @Override
    public void delete(TenantId tenantId, TenantProfile tenantProfile) throws ThingsboardException {
        tenantProfileService.deleteTenantProfile(tenantId, tenantProfile.getId());
        tbClusterService.onTenantProfileDelete(tenantProfile, null);
    }
}

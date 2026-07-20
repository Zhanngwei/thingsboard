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
package org.thingsboard.server.service.edge.rpc.processor.entityview;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

/**
 * 中文说明：
 * 1. `BaseEntityViewProcessor` 是 ThingsBoard Application 中处理实体视图的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class BaseEntityViewProcessor extends BaseEdgeProcessor {

    /**
     * 功能：保存或创建实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `entityViewUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    protected Pair<Boolean, Boolean> saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg) {
        boolean created = false;
        boolean entityViewNameUpdated = false;
        EntityView entityView = constructEntityViewFromUpdateMsg(tenantId, entityViewId, entityViewUpdateMsg);
        if (entityView == null) {
            throw new RuntimeException("[{" + tenantId + "}] entityViewUpdateMsg {" + entityViewUpdateMsg + "} cannot be converted to entity view");
        }
        EntityView entityViewById = entityViewService.findEntityViewById(tenantId, entityViewId);
        if (entityViewById == null) {
            created = true;
            entityView.setId(null);
        } else {
            entityView.setId(entityViewId);
        }
        String entityViewName = entityView.getName();
        EntityView entityViewByName = entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName);
        if (entityViewByName != null && !entityViewByName.getId().equals(entityViewId)) {
            entityViewName = entityViewName + "_" + StringUtils.randomAlphanumeric(15);
            log.warn("[{}] Entity view with name {} already exists. Renaming entity view name to {}",
                    tenantId, entityView.getName(), entityViewName);
            entityViewNameUpdated = true;
        }
        entityView.setName(entityViewName);
        setCustomerId(tenantId, created ? null : entityViewById.getCustomerId(), entityView, entityViewUpdateMsg);

        entityViewValidator.validate(entityView, EntityView::getTenantId);
        if (created) {
            entityView.setId(entityViewId);
        }
        entityViewService.saveEntityView(entityView, false);
        return Pair.of(created, entityViewNameUpdated);
    }

    /**
     * 功能：执行 `constructEntityViewFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `entityViewUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    protected abstract EntityView constructEntityViewFromUpdateMsg(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg);

    /**
     * 功能：更新客户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityView`：实体对象。
     * - `entityViewUpdateMsg`：待处理消息。
     * 返回：无。
     */
    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, EntityView entityView, EntityViewUpdateMsg entityViewUpdateMsg);
}

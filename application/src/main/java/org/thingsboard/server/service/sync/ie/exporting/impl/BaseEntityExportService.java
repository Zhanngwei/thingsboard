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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. `BaseEntityExportService` 是 ThingsBoard Application 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `EntityId`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
public abstract class BaseEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> extends DefaultEntityExportService<I, E, D> {

    /**
     * 功能：更新数据。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entity`：实体对象。
     * - `exportData`：待处理数据。
     * 返回：无。
     */
    @Override
    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, E entity, D exportData) throws ThingsboardException {
        setRelatedEntities(ctx, entity, (D) exportData);
        super.setAdditionalExportData(ctx, entity, exportData);
    }

    /**
     * 功能：更新`Related Entities`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mainEntity`：实体对象。
     * - `exportData`：待处理数据。
     * 返回：无。
     */
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, E mainEntity, D exportData) {
    }

    /**
     * 功能：执行 `newExportData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public abstract Set<EntityType> getSupportedEntityTypes();

    /**
     * 功能：执行 `replaceUuidsRecursively` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `node`：`node` 参数。
     * - `skippedRootFields`：`skippedRootFields` 参数。
     * - `includedFieldsPattern`：`includedFieldsPattern` 参数。
     * 返回：无。
     */
    protected void replaceUuidsRecursively(EntitiesExportCtx<?> ctx, JsonNode node, Set<String> skippedRootFields, Pattern includedFieldsPattern) {
        JacksonUtil.replaceUuidsRecursively(node, skippedRootFields, includedFieldsPattern, uuid -> getExternalIdOrElseInternalByUuid(ctx, uuid), true);
    }

    /**
     * 功能：执行 `toExternalIds` 对应的处理。
     * 参数：
     * - `internalIds`：数据列表。
     * - `entityIdCreator`：实体对象。
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    protected Stream<UUID> toExternalIds(Collection<UUID> internalIds, Function<UUID, EntityId> entityIdCreator,
                                         EntitiesExportCtx<?> ctx) {
        return internalIds.stream().map(entityIdCreator)
                .map(entityId -> getExternalIdOrElseInternal(ctx, entityId))
                .map(EntityId::getId);
    }

}

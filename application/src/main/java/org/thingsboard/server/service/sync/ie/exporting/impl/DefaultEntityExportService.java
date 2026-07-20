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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.sync.ie.AttributeExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.ie.exporting.EntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DefaultEntityExportService` 是 ThingsBoard Application 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `EntityId`、`EntityExportService`、`E`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@Primary
public class DefaultEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    protected ExportableEntitiesService exportableEntitiesService;
    /**
     * 关系，用于读取或保存对应领域对象。
     */
    @Autowired
    private RelationDao relationDao;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AttributesService attributesService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected ImageService imageService;

    /**
     * 功能：获取数据。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Override
    public final D getExportData(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entityId);
        if (entity == null) {
            throw new IllegalArgumentException(entityId.getEntityType() + " [" + entityId.getId() + "] not found");
        }

        exportData.setEntity(entity);
        exportData.setEntityType(entityId.getEntityType());
        setAdditionalExportData(ctx, entity, exportData);

        var externalId = entity.getExternalId() != null ? entity.getExternalId() : entity.getId();
        ctx.putExternalId(entityId, externalId);
        entity.setId(externalId);
        entity.setTenantId(null);

        return exportData;
    }

    /**
     * 功能：更新数据。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entity`：实体对象。
     * - `exportData`：待处理数据。
     * 返回：无。
     */
    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, E entity, D exportData) throws ThingsboardException {
        var exportSettings = ctx.getSettings();
        if (exportSettings.isExportRelations()) {
            List<EntityRelation> relations = exportRelations(ctx, entity);
            relations.forEach(relation -> {
                relation.setFrom(getExternalIdOrElseInternal(ctx, relation.getFrom()));
                relation.setTo(getExternalIdOrElseInternal(ctx, relation.getTo()));
            });
            exportData.setRelations(relations);
        }
        if (exportSettings.isExportAttributes()) {
            Map<String, List<AttributeExportData>> attributes = exportAttributes(ctx, entity);
            exportData.setAttributes(attributes);
        }
    }

    /**
     * 功能：执行 `exportRelations` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    private List<EntityRelation> exportRelations(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException {
        List<EntityRelation> relations = new ArrayList<>();

        List<EntityRelation> inboundRelations = relationDao.findAllByTo(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(inboundRelations);

        List<EntityRelation> outboundRelations = relationDao.findAllByFrom(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(outboundRelations);
        return relations;
    }

    /**
     * 功能：执行 `exportAttributes` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    private Map<String, List<AttributeExportData>> exportAttributes(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException {
        List<String> scopes;
        if (entity.getId().getEntityType() == EntityType.DEVICE) {
            scopes = List.of(DataConstants.SERVER_SCOPE, DataConstants.SHARED_SCOPE);
        } else {
            scopes = Collections.singletonList(DataConstants.SERVER_SCOPE);
        }
        Map<String, List<AttributeExportData>> attributes = new LinkedHashMap<>();
        scopes.forEach(scope -> {
            try {
                attributes.put(scope, attributesService.findAll(ctx.getTenantId(), entity.getId(), scope).get().stream()
                        .map(attribute -> {
                            AttributeExportData attributeExportData = new AttributeExportData();
                            attributeExportData.setKey(attribute.getKey());
                            attributeExportData.setLastUpdateTs(attribute.getLastUpdateTs());
                            attributeExportData.setStrValue(attribute.getStrValue().orElse(null));
                            attributeExportData.setDoubleValue(attribute.getDoubleValue().orElse(null));
                            attributeExportData.setLongValue(attribute.getLongValue().orElse(null));
                            attributeExportData.setBooleanValue(attribute.getBooleanValue().orElse(null));
                            attributeExportData.setJsonValue(attribute.getJsonValue().orElse(null));
                            return attributeExportData;
                        })
                        .collect(Collectors.toList()));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return attributes;
    }

    /**
     * 功能：获取`External Id Or Else Internal`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    protected <ID extends EntityId> ID getExternalIdOrElseInternal(EntitiesExportCtx<?> ctx, ID internalId) {
        if (internalId == null || internalId.isNullUid()) return internalId;
        var result = ctx.getExternalId(internalId);
        if (result == null) {
            result = Optional.ofNullable(exportableEntitiesService.getExternalIdByInternal(internalId))
                    .orElse(internalId);
            ctx.putExternalId(internalId, result);
        }
        return result;
    }

    /**
     * 功能：获取`External Id Or Else Internal By Uuid`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `internalUuid`：`internalUuid`ID。
     * 返回：处理结果。
     */
    protected UUID getExternalIdOrElseInternalByUuid(EntitiesExportCtx<?> ctx, UUID internalUuid) {
        for (EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = ctx.getExternalId(internalId);
            if (externalId != null) {
                return externalId.getId();
            }
        }
        for (EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = exportableEntitiesService.getExternalIdByInternal(internalId);
            if (externalId != null) {
                ctx.putExternalId(internalId, externalId);
                return externalId.getId();
            }
        }
        return internalUuid;
    }

    /**
     * 功能：执行 `newExportData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

}

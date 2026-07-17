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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Objects;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.sync.ie.AttributeExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.ie.importing.EntityImportService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `BaseEntityImportService` 是 ThingsBoard Application 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `EntityId`、`EntityImportService`、`E`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
public abstract class BaseEntityImportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityImportService<I, E, D> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private ExportableEntitiesService exportableEntitiesService;
    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    private RelationService relationService;
    /**
     * 关系，用于读取或保存对应领域对象。
     */
    @Autowired
    private RelationDao relationDao;
    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired
    private TelemetrySubscriptionService tsSubService;
    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    protected EntityActionService entityActionService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbClusterService clusterService;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbNotificationEntityService entityNotificationService;

    /**
     * 功能：执行 `importEntity` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `exportData`：待处理数据。
     * 返回：处理结果。
     */
    @Override
    public EntityImportResult<E> importEntity(EntitiesImportCtx ctx, D exportData) throws ThingsboardException {
        EntityImportResult<E> importResult = new EntityImportResult<>();
        ctx.setCurrentImportResult(importResult);
        importResult.setEntityType(getEntityType());
        IdProvider idProvider = new IdProvider(ctx, importResult);

        E entity = exportData.getEntity();
        entity.setExternalId(entity.getId());

        E existingEntity = findExistingEntity(ctx, entity, idProvider);
        importResult.setOldEntity(existingEntity);

        setOwner(ctx.getTenantId(), entity, idProvider);
        if (existingEntity == null) {
            entity.setId(null);
        } else {
            entity.setId(existingEntity.getId());
            entity.setCreatedTime(existingEntity.getCreatedTime());
        }

        E prepared = prepare(ctx, entity, existingEntity, exportData, idProvider);

        boolean saveOrUpdate = existingEntity == null || compare(ctx, exportData, prepared, existingEntity);

        if (saveOrUpdate) {
            E savedEntity = saveOrUpdate(ctx, prepared, exportData, idProvider);
            boolean created = existingEntity == null;
            importResult.setCreated(created);
            importResult.setUpdated(!created);
            importResult.setSavedEntity(savedEntity);
            ctx.putInternalId(exportData.getExternalId(), savedEntity.getId());
        } else {
            importResult.setSavedEntity(existingEntity);
            ctx.putInternalId(exportData.getExternalId(), existingEntity.getId());
            importResult.setUpdatedRelatedEntities(updateRelatedEntitiesIfUnmodified(ctx, prepared, exportData, idProvider));
        }

        processAfterSaved(ctx, importResult, exportData, idProvider);

        return importResult;
    }

    /**
     * 功能：更新`Related Entities If Unmodified`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `prepared`：`prepared` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：判断结果。
     */
    protected boolean updateRelatedEntitiesIfUnmodified(EntitiesImportCtx ctx, E prepared, D exportData, IdProvider idProvider) {
        return false;
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public abstract EntityType getEntityType();

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    protected abstract void setOwner(TenantId tenantId, E entity, IdProvider idProvider);

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entity`：实体对象。
     * - `oldEntity`：实体对象。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected abstract E prepare(EntitiesImportCtx ctx, E entity, E oldEntity, D exportData, IdProvider idProvider);

    /**
     * 功能：执行 `compare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `exportData`：待处理数据。
     * - `prepared`：`prepared` 参数。
     * - `existing`：`existing` 参数。
     * 返回：判断结果。
     */
    protected boolean compare(EntitiesImportCtx ctx, D exportData, E prepared, E existing) {
        var newCopy = deepCopy(prepared);
        var existingCopy = deepCopy(existing);
        cleanupForComparison(newCopy);
        cleanupForComparison(existingCopy);
        var result = !newCopy.equals(existingCopy);
        if (result) {
            log.debug("[{}] Found update.", prepared.getId());
            log.debug("[{}] From: {}", prepared.getId(), newCopy);
            log.debug("[{}] To: {}", prepared.getId(), existingCopy);
        }
        return result;
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：处理结果。
     */
    protected abstract E deepCopy(E e);

    /**
     * 功能：删除或清理`For Comparison`。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    protected void cleanupForComparison(E e) {
        e.setTenantId(null);
        e.setCreatedTime(0);
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entity`：实体对象。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    protected abstract E saveOrUpdate(EntitiesImportCtx ctx, E entity, D exportData, IdProvider idProvider);


    /**
     * 功能：处理`After Saved`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `importResult`：`importResult` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    protected void processAfterSaved(EntitiesImportCtx ctx, EntityImportResult<E> importResult, D exportData, IdProvider idProvider) throws ThingsboardException {
        E savedEntity = importResult.getSavedEntity();
        E oldEntity = importResult.getOldEntity();

        if (importResult.isCreated() || importResult.isUpdated()) {
            importResult.addSendEventsCallback(() -> onEntitySaved(ctx.getUser(), savedEntity, oldEntity));
        }

        if (ctx.isUpdateRelations() && exportData.getRelations() != null) {
            importRelations(ctx, exportData.getRelations(), importResult, idProvider);
        }
        if (ctx.isSaveAttributes() && exportData.getAttributes() != null) {
            if (exportData.getAttributes().values().stream().anyMatch(d -> !d.isEmpty())) {
                importResult.setUpdatedRelatedEntities(true);
            }
            importAttributes(ctx.getUser(), exportData.getAttributes(), importResult);
        }
    }

    /**
     * 功能：执行 `importRelations` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `relations`：数据列表。
     * - `importResult`：`importResult` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    private void importRelations(EntitiesImportCtx ctx, List<EntityRelation> relations, EntityImportResult<E> importResult, IdProvider idProvider) {
        var tenantId = ctx.getTenantId();
        E entity = importResult.getSavedEntity();
        importResult.addSaveReferencesCallback(() -> {
            for (EntityRelation relation : relations) {
                if (!relation.getTo().equals(entity.getId())) {
                    relation.setTo(idProvider.getInternalId(relation.getTo()));
                }
                if (!relation.getFrom().equals(entity.getId())) {
                    relation.setFrom(idProvider.getInternalId(relation.getFrom()));
                }
            }

            Map<EntityRelation, EntityRelation> relationsMap = new LinkedHashMap<>();
            relations.forEach(r -> relationsMap.put(r, r));

            if (importResult.getOldEntity() != null) {
                List<EntityRelation> existingRelations = new ArrayList<>();
                existingRelations.addAll(relationDao.findAllByTo(tenantId, entity.getId(), RelationTypeGroup.COMMON));
                existingRelations.addAll(relationDao.findAllByFrom(tenantId, entity.getId(), RelationTypeGroup.COMMON));
                // dao is used here instead of service to avoid getting cached values, because relationService.deleteRelation will evict value from cache only after transaction is committed

                for (EntityRelation existingRelation : existingRelations) {
                    EntityRelation relation = relationsMap.get(existingRelation);
                    if (relation == null) {
                        importResult.setUpdatedRelatedEntities(true);
                        relationService.deleteRelation(ctx.getTenantId(), existingRelation.getFrom(), existingRelation.getTo(), existingRelation.getType(), existingRelation.getTypeGroup());
                        importResult.addSendEventsCallback(() -> {
                            entityNotificationService.logEntityRelationAction(tenantId, null,
                                    existingRelation, ctx.getUser(), ActionType.RELATION_DELETED, null, existingRelation);
                        });
                    } else if (Objects.equal(relation.getAdditionalInfo(), existingRelation.getAdditionalInfo())) {
                        relationsMap.remove(relation);
                    }
                }
            }
            if (!relationsMap.isEmpty()) {
                importResult.setUpdatedRelatedEntities(true);
                ctx.addRelations(relationsMap.values());
            }
        });
    }

    /**
     * 功能：执行 `importAttributes` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `attributes`：数据列表。
     * - `importResult`：`importResult` 参数。
     * 返回：无。
     */
    private void importAttributes(User user, Map<String, List<AttributeExportData>> attributes, EntityImportResult<E> importResult) {
        E entity = importResult.getSavedEntity();
        importResult.addSaveReferencesCallback(() -> {
            attributes.forEach((scope, attributesExportData) -> {
                List<AttributeKvEntry> attributeKvEntries = attributesExportData.stream()
                        .map(attributeExportData -> {
                            KvEntry kvEntry;
                            String key = attributeExportData.getKey();
                            if (attributeExportData.getStrValue() != null) {
                                kvEntry = new StringDataEntry(key, attributeExportData.getStrValue());
                            } else if (attributeExportData.getBooleanValue() != null) {
                                kvEntry = new BooleanDataEntry(key, attributeExportData.getBooleanValue());
                            } else if (attributeExportData.getDoubleValue() != null) {
                                kvEntry = new DoubleDataEntry(key, attributeExportData.getDoubleValue());
                            } else if (attributeExportData.getLongValue() != null) {
                                kvEntry = new LongDataEntry(key, attributeExportData.getLongValue());
                            } else if (attributeExportData.getJsonValue() != null) {
                                kvEntry = new JsonDataEntry(key, attributeExportData.getJsonValue());
                            } else {
                                throw new IllegalArgumentException("Invalid attribute export data");
                            }
                            return new BaseAttributeKvEntry(kvEntry, attributeExportData.getLastUpdateTs());
                        })
                        .collect(Collectors.toList());
                // fixme: attributes are saved outside the transaction
                tsSubService.saveAndNotify(user.getTenantId(), entity.getId(), scope, attributeKvEntries, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void unused) {
                    }

                    @Override
                    public void onFailure(Throwable thr) {
                        log.error("Failed to import attributes for {} {}", entity.getId().getEntityType(), entity.getId(), thr);
                    }
                });
            });
        });
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `savedEntity`：实体对象。
     * - `oldEntity`：实体对象。
     * 返回：无。
     */
    protected void onEntitySaved(User user, E savedEntity, E oldEntity) throws ThingsboardException {
        entityNotificationService.logEntityAction(user.getTenantId(), savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }


    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entity`：实体对象。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    protected E findExistingEntity(EntitiesImportCtx ctx, E entity, IdProvider idProvider) {
        return (E) Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndExternalId(ctx.getTenantId(), entity.getId()))
                .or(() -> Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entity.getId())))
                .or(() -> {
                    if (ctx.isFindExistingByName()) {
                        return Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndName(ctx.getTenantId(), getEntityType(), entity.getName()));
                    } else {
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    private <ID extends EntityId> HasId<ID> findInternalEntity(TenantId tenantId, ID externalId) {
        return (HasId<ID>) Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndExternalId(tenantId, externalId))
                .or(() -> Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndId(tenantId, externalId)))
                .orElseThrow(() -> new MissingEntityException(externalId));
    }


    /**
     * 中文说明：
     * 1. `IdProvider` 是 ThingsBoard Application 中创建或提供 `Id Provider` 对象的构造组件。
     * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
     * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
     * 4. 它直接协作于目标接口、具体实现和创建所需配置。
     * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
     * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
     */
    @SuppressWarnings("unchecked")
    @RequiredArgsConstructor
    protected class IdProvider {
        /**
         * 上下文，汇总当前处理所需的上下文信息。
         */
        private final EntitiesImportCtx ctx;
        private final EntityImportResult<E> importResult;

        /**
         * 功能：获取`Internal Id`。
         * 参数：
         * - `externalId`：`externalId`ID。
         * 返回：处理结果。
         */
        public <ID extends EntityId> ID getInternalId(ID externalId) {
            return getInternalId(externalId, true);
        }

        /**
         * 功能：获取`Internal Id`。
         * 参数：
         * - `externalId`：`externalId`ID。
         * - `throwExceptionIfNotFound`：`throwExceptionIfNotFound` 参数。
         * 返回：处理结果。
         */
        public <ID extends EntityId> ID getInternalId(ID externalId, boolean throwExceptionIfNotFound) {
            if (externalId == null || externalId.isNullUid()) return null;

            if (EntityType.TENANT.equals(externalId.getEntityType())) {
                return (ID) ctx.getTenantId();
            }

            EntityId localId = ctx.getInternalId(externalId);
            if (localId != null) {
                return (ID) localId;
            }

            HasId<ID> entity;
            try {
                entity = findInternalEntity(ctx.getTenantId(), externalId);
            } catch (Exception e) {
                if (throwExceptionIfNotFound) {
                    throw e;
                } else {
                    importResult.setUpdatedAllExternalIds(false);
                    return null;
                }
            }
            ctx.putInternalId(externalId, entity.getId());
            return entity.getId();
        }

        /**
         * 功能：获取`Internal Id By Uuid`。
         * 参数：
         * - `externalUuid`：`externalUuid`ID。
         * - `fetchAllUUIDs`：`fetchAllUUIDs` 参数。
         * - `hints`：`hints` 参数。
         * 返回：可能存在的结果。
         */
        public Optional<EntityId> getInternalIdByUuid(UUID externalUuid, boolean fetchAllUUIDs, Set<EntityType> hints) {
            if (externalUuid.equals(EntityId.NULL_UUID)) return Optional.empty();

            for (EntityType entityType : EntityType.values()) {
                Optional<EntityId> externalId = buildEntityId(entityType, externalUuid);
                if (externalId.isEmpty()) {
                    continue;
                }
                EntityId internalId = ctx.getInternalId(externalId.get());
                if (internalId != null) {
                    return Optional.of(internalId);
                }
            }

            if (fetchAllUUIDs) {
                Set<EntityType> processLast = Set.of(EntityType.TENANT);
                List<EntityType> entityTypes = new ArrayList<>(hints);
                for (EntityType entityType : EntityType.values()) {
                    if (!hints.contains(entityType) && !processLast.contains(entityType)) {
                        entityTypes.add(entityType);
                    }
                }
                entityTypes.addAll(processLast);

                for (EntityType entityType : entityTypes) {
                    Optional<EntityId> externalId = buildEntityId(entityType, externalUuid);
                    if (externalId.isEmpty() || ctx.isNotFound(externalId.get())) {
                        continue;
                    }
                    EntityId internalId = getInternalId(externalId.get(), false);
                    if (internalId != null) {
                        return Optional.of(internalId);
                    } else {
                        ctx.registerNotFound(externalId.get());
                    }
                }
            }

            importResult.setUpdatedAllExternalIds(false);
            return Optional.empty();
        }

        /**
         * 功能：构建实体ID。
         * 参数：
         * - `entityType`：实体对象。
         * - `externalUuid`：`externalUuid`ID。
         * 返回：可能存在的结果。
         */
        private Optional<EntityId> buildEntityId(EntityType entityType, UUID externalUuid) {
            try {
                return Optional.of(EntityIdFactory.getByTypeAndUuid(entityType, externalUuid));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `oldEntity`：实体对象。
     * - `getter`：`getter` 参数。
     * 返回：处理结果。
     */
    protected <T extends EntityId, O> T getOldEntityField(O oldEntity, Function<O, T> getter) {
        return oldEntity == null ? null : getter.apply(oldEntity);
    }

    /**
     * 功能：执行 `replaceIdsRecursively` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `idProvider`：`idProvider` 参数。
     * - `json`：`json` 参数。
     * - `skippedRootFields`：`skippedRootFields` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void replaceIdsRecursively(EntitiesImportCtx ctx, IdProvider idProvider, JsonNode json,
                                         Set<String> skippedRootFields, Pattern includedFieldsPattern,
                                         LinkedHashSet<EntityType> hints) {
        JacksonUtil.replaceUuidsRecursively(json, skippedRootFields, includedFieldsPattern,
                uuid -> idProvider.getInternalIdByUuid(uuid, ctx.isFinalImportAttempt(), hints)
                        .map(EntityId::getId).orElse(uuid), true);
    }

}

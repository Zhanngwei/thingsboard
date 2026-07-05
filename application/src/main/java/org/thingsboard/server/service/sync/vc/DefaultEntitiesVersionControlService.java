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
package org.thingsboard.server.service.sync.vc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.TbStopWatch;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityDataDiff;
import org.thingsboard.server.common.data.sync.vc.EntityDataInfo;
import org.thingsboard.server.common.data.sync.vc.EntityLoadError;
import org.thingsboard.server.common.data.sync.vc.EntityTypeLoadResult;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.AutoVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.SingleEntityVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.VersionControlExecutor;
import org.thingsboard.server.service.sync.ie.EntitiesExportImportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.ie.importing.impl.MissingEntityException;
import org.thingsboard.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.thingsboard.server.service.sync.vc.data.CommitGitRequest;
import org.thingsboard.server.service.sync.vc.data.ComplexEntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.sync.vc.data.EntityTypeExportCtx;
import org.thingsboard.server.service.sync.vc.data.ReimportTask;
import org.thingsboard.server.service.sync.vc.data.SimpleEntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.repository.TbRepositorySettingsService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Futures.transform;
import static org.thingsboard.server.common.data.sync.vc.VcUtils.checkBranchName;

/**
 * 中文说明：
 * 1. 类目的：`DefaultEntitiesVersionControlService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesVersionControlService implements EntitiesVersionControlService {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbRepositorySettingsService repositorySettingsService;
    private final TbAutoCommitSettingsService autoCommitSettingsService;
    /**
     * 队列，提供当前类调用的业务操作。
     */
    private final GitVersionControlQueueService gitServiceQueue;
    private final EntitiesExportImportService exportImportService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ExportableEntitiesService exportableEntitiesService;
    private final TbNotificationEntityService entityNotificationService;
    /**
     * 事务执行模板，表示当前对象的对应属性。
     */
    private final TransactionTemplate transactionTemplate;
    private final TbTransactionalCache<UUID, VersionControlTaskCacheEntry> taskCache;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final VersionControlExecutor executor;

    /**
     * 功能：保存或创建版本号。
     * 参数：
     * - `user`：`user` 参数。
     * - `request`：请求对象。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ListenableFuture<UUID> saveEntitiesVersion(User user, VersionCreateRequest request) throws Exception {
        checkBranchName(request.getBranch());
        var pendingCommit = gitServiceQueue.prepareCommit(user, request);
        DonAsynchron.withCallback(pendingCommit, commit -> {
            cachePut(commit.getTxId(), new VersionCreationResult());
            try {
                EntitiesExportCtx<?> theCtx;
                switch (request.getType()) {
                    case SINGLE_ENTITY: {
                        var ctx = new SimpleEntitiesExportCtx(user, commit, (SingleEntityVersionCreateRequest) request);
                        handleSingleEntityRequest(ctx);
                        theCtx = ctx;
                        break;
                    }
                    case COMPLEX: {
                        var ctx = new ComplexEntitiesExportCtx(user, commit, (ComplexVersionCreateRequest) request);
                        handleComplexRequest(ctx);
                        theCtx = ctx;
                        break;
                    }
                    default:
                        throw new RuntimeException("Unsupported request type: " + request.getType());
                }
                var resultFuture = Futures.transformAsync(Futures.allAsList(theCtx.getFutures()), f -> gitServiceQueue.push(commit), executor);
                DonAsynchron.withCallback(resultFuture, result -> cachePut(commit.getTxId(), result), e -> processCommitError(user, request, commit, e), executor);
            } catch (Exception e) {
                processCommitError(user, request, commit, e);
            }
        }, t -> log.debug("[{}] Failed to prepare the commit: {}", user.getId(), request, t));

        return transform(pendingCommit, CommitGitRequest::getTxId, MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `user`：`user` 参数。
     * - `requestId`：请求ID。
     * 返回：处理结果。
     */
    @Override
    public VersionCreationResult getVersionCreateStatus(User user, UUID requestId) throws ThingsboardException {
        return getStatus(user, requestId, VersionControlTaskCacheEntry::getExportResult);
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `user`：`user` 参数。
     * - `requestId`：请求ID。
     * 返回：处理结果。
     */
    @Override
    public VersionLoadResult getVersionLoadStatus(User user, UUID requestId) throws ThingsboardException {
        return getStatus(user, requestId, VersionControlTaskCacheEntry::getImportResult);
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `user`：`user` 参数。
     * - `requestId`：请求ID。
     * - `getter`：`getter` 参数。
     * 返回：处理结果。
     */
    private <T> T getStatus(User user, UUID requestId, Function<VersionControlTaskCacheEntry, T> getter) throws ThingsboardException {
        var cacheEntry = taskCache.get(requestId);
        if (cacheEntry == null || cacheEntry.get() == null) {
            log.debug("[{}] No cache record: {}", requestId, cacheEntry);
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        } else {
            var entry = cacheEntry.get();
            log.trace("[{}] Cache get: {}", requestId, entry);
            var result = getter.apply(entry);
            if (result == null) {
                throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else {
                return result;
            }
        }
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    private void handleSingleEntityRequest(SimpleEntitiesExportCtx ctx) throws Exception {
        ctx.add(saveEntityData(ctx, ctx.getRequest().getEntityId()));
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `parentCtx`：处理上下文。
     * 返回：无。
     */
    private void handleComplexRequest(ComplexEntitiesExportCtx parentCtx) {
        ComplexVersionCreateRequest request = parentCtx.getRequest();
        request.getEntityTypes().forEach((entityType, config) -> {
            EntityTypeExportCtx ctx = new EntityTypeExportCtx(parentCtx, config, request.getSyncStrategy(), entityType);
            if (ctx.isOverwrite()) {
                ctx.add(gitServiceQueue.deleteAll(ctx.getCommit(), entityType));
            }

            if (config.isAllEntities()) {
                DaoUtil.processInBatches(pageLink -> exportableEntitiesService.findEntitiesIdsByTenantId(ctx.getTenantId(), entityType, pageLink),
                        100, entityId -> {
                            try {
                                ctx.add(saveEntityData(ctx, entityId));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            } else {
                for (UUID entityId : config.getEntityIds()) {
                    try {
                        ctx.add(saveEntityData(ctx, EntityIdFactory.getByTypeAndUuid(entityType, entityId)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> saveEntityData(EntitiesExportCtx<?> ctx, EntityId entityId) throws Exception {
        EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(ctx, entityId);
        return gitServiceQueue.addToCommit(ctx.getCommit(), entityData);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `externalId`：`externalId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<PageData<EntityVersion>> listEntityVersions(TenantId tenantId, String branch, EntityId externalId, PageLink pageLink) throws Exception {
        return gitServiceQueue.listVersions(tenantId, branch, externalId, pageLink);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `entityType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<PageData<EntityVersion>> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) throws Exception {
        return gitServiceQueue.listVersions(tenantId, branch, entityType, pageLink);
    }

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink) throws Exception {
        return gitServiceQueue.listVersions(tenantId, branch, pageLink);
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * - `entityType`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType) throws Exception {
        return gitServiceQueue.listEntitiesAtVersion(tenantId, versionId, entityType);
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listAllEntitiesAtVersion(TenantId tenantId, String versionId) throws Exception {
        return gitServiceQueue.listEntitiesAtVersion(tenantId, versionId);
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `user`：`user` 参数。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    @SuppressWarnings({"rawtypes"})
    @Override
    public UUID loadEntitiesVersion(User user, VersionLoadRequest request) throws Exception {
        EntitiesImportCtx ctx = new EntitiesImportCtx(UUID.randomUUID(), user, request.getVersionId());
        cachePut(ctx.getRequestId(), VersionLoadResult.empty());
        switch (request.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionLoadRequest versionLoadRequest = (SingleEntityVersionLoadRequest) request;
                VersionLoadConfig config = versionLoadRequest.getConfig();
                ListenableFuture<EntityExportData> future = gitServiceQueue.getEntity(user.getTenantId(), request.getVersionId(), versionLoadRequest.getExternalEntityId());
                DonAsynchron.withCallback(future,
                        entityData -> doInTemplate(ctx, request, c -> loadSingleEntity(c, config, entityData)),
                        e -> processLoadError(ctx, e), executor);
                break;
            }
            case ENTITY_TYPE: {
                EntityTypeVersionLoadRequest versionLoadRequest = (EntityTypeVersionLoadRequest) request;
                executor.submit(() -> doInTemplate(ctx, request, c -> loadMultipleEntities(c, versionLoadRequest)));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported version load request");
        }

        return ctx.getRequestId();
    }

    /**
     * 功能：执行 `doInTemplate` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `request`：请求对象。
     * - `function`：`function` 参数。
     * 返回：处理结果。
     */
    private <R> VersionLoadResult doInTemplate(EntitiesImportCtx ctx, VersionLoadRequest request, Function<EntitiesImportCtx, VersionLoadResult> function) {
        try {
            VersionLoadResult result = transactionTemplate.execute(status -> {
                try {
                    return function.apply(ctx);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e); // to prevent UndeclaredThrowableException
                }
            });
            for (ThrowingRunnable throwingRunnable : ctx.getEventCallbacks()) {
                throwingRunnable.run();
            }
            result.setDone(true);
            return cachePut(ctx.getRequestId(), result);
        } catch (LoadEntityException e) {
            return cachePut(ctx.getRequestId(), onError(e.getExternalId(), e.getCause()));
        } catch (Exception e) {
            log.info("[{}] Failed to process request [{}] due to: ", ctx.getTenantId(), request, e);
            return cachePut(ctx.getRequestId(), VersionLoadResult.error(EntityLoadError.runtimeError(e)));
        }
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `config`：配置对象。
     * - `entityData`：待处理数据。
     * 返回：处理结果。
     */
    private VersionLoadResult loadSingleEntity(EntitiesImportCtx ctx, VersionLoadConfig config, EntityExportData entityData) {
        try {
            ctx.setSettings(EntityImportSettings.builder()
                    .updateRelations(config.isLoadRelations())
                    .saveAttributes(config.isLoadAttributes())
                    .saveCredentials(config.isLoadCredentials())
                    .findExistingByName(false)
                    .build());
            ctx.setFinalImportAttempt(true);
            EntityImportResult<?> importResult = exportImportService.importEntity(ctx, entityData);

            exportImportService.saveReferencesAndRelations(ctx);

            return VersionLoadResult.success(EntityTypeLoadResult.builder()
                    .entityType(importResult.getEntityType())
                    .created(importResult.getOldEntity() == null ? 1 : 0)
                    .updated(importResult.getOldEntity() != null ? 1 : 0)
                    .deleted(0)
                    .build());
        } catch (Exception e) {
            throw new LoadEntityException(entityData.getExternalId(), e);
        }
    }

    /**
     * 功能：获取`Multiple Entities`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    @SneakyThrows
    private VersionLoadResult loadMultipleEntities(EntitiesImportCtx ctx, EntityTypeVersionLoadRequest request) {
        var sw = TbStopWatch.create("before");

        List<EntityType> entityTypes = request.getEntityTypes().keySet().stream()
                .sorted(exportImportService.getEntityTypeComparatorForImport()).collect(Collectors.toList());
        for (EntityType entityType : entityTypes) {
            log.debug("[{}] Loading {} entities", ctx.getTenantId(), entityType);
            sw.startNew("Entities " + entityType.name());
            ctx.setSettings(getEntityImportSettings(request, entityType));
            importEntities(ctx, entityType);
            persistToCache(ctx);
        }

        sw.startNew("Reimport");
        reimport(ctx);
        persistToCache(ctx);

        sw.startNew("Remove Others");
        request.getEntityTypes().keySet().stream()
                .filter(entityType -> request.getEntityTypes().get(entityType).isRemoveOtherEntities())
                .sorted(exportImportService.getEntityTypeComparatorForImport().reversed())
                .forEach(entityType -> removeOtherEntities(ctx, entityType));
        persistToCache(ctx);

        sw.startNew("References and Relations");
        exportImportService.saveReferencesAndRelations(ctx);

        sw.stop();
        for (var task : sw.getTaskInfo()) {
            log.debug("[{}] Executed: {} in {}ms", ctx.getTenantId(), task.getTaskName(), task.getTimeMillis());
        }
        log.debug("[{}] Total time: {}ms", ctx.getTenantId(), sw.getTotalTimeMillis());
        return VersionLoadResult.success(new ArrayList<>(ctx.getResults().values()));
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `request`：请求对象。
     * - `entityType`：实体对象。
     * 返回：匹配的数据集合。
     */
    private EntityImportSettings getEntityImportSettings(EntityTypeVersionLoadRequest request, EntityType entityType) {
        var config = request.getEntityTypes().get(entityType);
        return EntityImportSettings.builder()
                .updateRelations(config.isLoadRelations())
                .saveAttributes(config.isLoadAttributes())
                .saveCredentials(config.isLoadCredentials())
                .findExistingByName(config.isFindExistingEntityByName())
                .build();
    }

    /**
     * 功能：执行 `importEntities` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    @SneakyThrows
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void importEntities(EntitiesImportCtx ctx, EntityType entityType) {
        int limit = 100;
        int offset = 0;
        List<EntityExportData> entityDataList;
        do {
            try {
                entityDataList = gitServiceQueue.getEntities(ctx.getTenantId(), ctx.getVersionId(), entityType, offset, limit).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
            log.debug("[{}] Loading {} entities pack ({})", ctx.getTenantId(), entityType, entityDataList.size());
            for (EntityExportData entityData : entityDataList) {
                EntityExportData reimportBackup = JacksonUtil.clone(entityData);
                EntityImportResult<?> importResult;
                try {
                    importResult = exportImportService.importEntity(ctx, entityData);
                } catch (Exception e) {
                    throw new LoadEntityException(entityData.getExternalId(), e);
                }
                registerResult(ctx, entityType, importResult);

                if (!importResult.isUpdatedAllExternalIds()) {
                    ctx.getToReimport().put(entityData.getEntity().getExternalId(), new ReimportTask(reimportBackup, ctx.getSettings()));
                    continue;
                }
                ctx.getImportedEntities().computeIfAbsent(entityType, t -> new HashSet<>())
                        .add(importResult.getSavedEntity().getId());
            }
            log.debug("Imported {} pack ({}) for tenant {}", entityType, entityDataList.size(), ctx.getTenantId());
            offset += limit;
        } while (entityDataList.size() == limit);
    }

    /**
     * 功能：执行 `reimport` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void reimport(EntitiesImportCtx ctx) {
        ctx.setFinalImportAttempt(true);
        ctx.getToReimport().forEach((externalId, task) -> {
            try {
                EntityExportData entityData = task.getData();
                var settings = task.getSettings();
                ctx.setSettings(settings);
                EntityImportResult<?> importResult = exportImportService.importEntity(ctx, entityData);

                ctx.getImportedEntities().computeIfAbsent(externalId.getEntityType(), t -> new HashSet<>())
                        .add(importResult.getSavedEntity().getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 功能：删除或清理`Other Entities`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    private void removeOtherEntities(EntitiesImportCtx ctx, EntityType entityType) {
        DaoUtil.processInBatches(pageLink -> {
            return exportableEntitiesService.findEntitiesByTenantId(ctx.getTenantId(), entityType, pageLink);
        }, 100, entity -> {
            if (ctx.getImportedEntities().get(entityType) == null || !ctx.getImportedEntities().get(entityType).contains(entity.getId())) {
                exportableEntitiesService.removeById(ctx.getTenantId(), entity.getId());

                ctx.addEventCallback(() -> {
                    entityNotificationService.logEntityAction(ctx.getTenantId(), entity.getId(), entity, null,
                            ActionType.DELETED, ctx.getUser());
                });
                ctx.registerDeleted(entityType);
            }
        });
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `externalId`：`externalId`ID。
     * - `e`：`e` 参数。
     * 返回：处理结果。
     */
    private VersionLoadResult onError(EntityId externalId, Throwable e) {
        return analyze(e, externalId).orElse(VersionLoadResult.error(EntityLoadError.runtimeError(e)));
    }

    /**
     * 功能：执行 `analyze` 对应的处理。
     * 参数：
     * - `e`：`e` 参数。
     * - `externalId`：`externalId`ID。
     * 返回：可能存在的结果。
     */
    private Optional<VersionLoadResult> analyze(Throwable e, EntityId externalId) {
        if (e == null) {
            return Optional.empty();
        } else {
            if (e instanceof DeviceCredentialsValidationException) {
                return Optional.of(VersionLoadResult.error(EntityLoadError.credentialsError(externalId)));
            } else if (e instanceof MissingEntityException) {
                return Optional.of(VersionLoadResult.error(EntityLoadError.referenceEntityError(externalId, ((MissingEntityException) e).getEntityId())));
            } else {
                return analyze(e.getCause(), externalId);
            }
        }
    }

    /**
     * 功能：执行 `compareEntityDataToVersion` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<EntityDataDiff> compareEntityDataToVersion(User user, EntityId entityId, String versionId) throws Exception {
        HasId<EntityId> entity = exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), entityId);
        if (!(entity instanceof ExportableEntity)) throw new IllegalArgumentException("Unsupported entity type");

        EntityId externalId = ((ExportableEntity<EntityId>) entity).getExternalId();
        if (externalId == null) externalId = entityId;

        return transform(gitServiceQueue.getEntity(user.getTenantId(), versionId, externalId),
                otherVersion -> {
                    SimpleEntitiesExportCtx ctx = new SimpleEntitiesExportCtx(user, null, null, EntityExportSettings.builder()
                            .exportRelations(otherVersion.hasRelations())
                            .exportAttributes(otherVersion.hasAttributes())
                            .exportCredentials(otherVersion.hasCredentials())
                            .build());
                    EntityExportData<?> currentVersion;
                    try {
                        currentVersion = exportImportService.exportEntity(ctx, entityId);
                    } catch (ThingsboardException e) {
                        throw new RuntimeException(e);
                    }
                    return new EntityDataDiff(currentVersion.sort(), otherVersion.sort());
                }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<EntityDataInfo> getEntityDataInfo(User user, EntityId entityId, String versionId) {
        return Futures.transform(gitServiceQueue.getEntity(user.getTenantId(), versionId, entityId),
                entity -> new EntityDataInfo(entity.hasRelations(), entity.hasAttributes(), entity.hasCredentials()), MoreExecutors.directExecutor());
    }


    /**
     * 功能：获取`Branches`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId) throws Exception {
        return gitServiceQueue.listBranches(tenantId);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public RepositorySettings getVersionControlSettings(TenantId tenantId) {
        return repositorySettingsService.get(tenantId);
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionControlSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<RepositorySettings> saveVersionControlSettings(TenantId tenantId, RepositorySettings versionControlSettings) {
        checkBranchName(versionControlSettings.getDefaultBranch());
        var restoredSettings = this.repositorySettingsService.restore(tenantId, versionControlSettings);
        try {
            var future = gitServiceQueue.initRepository(tenantId, restoredSettings);
            return Futures.transform(future, f -> repositorySettingsService.save(tenantId, restoredSettings), MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.debug("{} Failed to init repository: {}", tenantId, versionControlSettings, e);
            throw new RuntimeException("Failed to init repository!", e);
        }
    }

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> deleteVersionControlSettings(TenantId tenantId) throws Exception {
        if (repositorySettingsService.delete(tenantId)) {
            return gitServiceQueue.clearRepository(tenantId);
        } else {
            return Futures.immediateFuture(null);
        }
    }

    /**
     * 功能：校验版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public ListenableFuture<Void> checkVersionControlAccess(TenantId tenantId, RepositorySettings settings) throws ThingsboardException {
        checkBranchName(settings.getDefaultBranch());
        settings = this.repositorySettingsService.restore(tenantId, settings);
        try {
            return gitServiceQueue.testRepository(tenantId, settings);
        } catch (Exception e) {
            throw new ThingsboardException(String.format("Unable to access repository: %s", getCauseMessage(e)),
                    ThingsboardErrorCode.GENERAL);
        }
    }

    /**
     * 功能：执行 `autoCommit` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception {
        var repositorySettings = repositorySettingsService.get(user.getTenantId());
        if (repositorySettings == null || repositorySettings.isReadOnly()) {
            return Futures.immediateFuture(null);
        }
        var autoCommitSettings = autoCommitSettingsService.get(user.getTenantId());
        if (autoCommitSettings == null) {
            return Futures.immediateFuture(null);
        }
        var entityType = entityId.getEntityType();
        AutoVersionCreateConfig autoCommitConfig = autoCommitSettings.get(entityType);
        if (autoCommitConfig == null) {
            return Futures.immediateFuture(null);
        }
        SingleEntityVersionCreateRequest vcr = new SingleEntityVersionCreateRequest();
        var autoCommitBranchName = autoCommitConfig.getBranch();
        if (StringUtils.isEmpty(autoCommitBranchName)) {
            autoCommitBranchName = StringUtils.isNotEmpty(repositorySettings.getDefaultBranch()) ? repositorySettings.getDefaultBranch() : "auto-commits";
        }
        vcr.setBranch(autoCommitBranchName);
        vcr.setVersionName("auto-commit at " + Instant.ofEpochSecond(System.currentTimeMillis() / 1000));
        vcr.setEntityId(entityId);
        vcr.setConfig(autoCommitConfig);
        return saveEntitiesVersion(user, vcr);
    }

    /**
     * 功能：执行 `autoCommit` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception {
        var repositorySettings = repositorySettingsService.get(user.getTenantId());
        if (repositorySettings == null || repositorySettings.isReadOnly()) {
            return Futures.immediateFuture(null);
        }
        var autoCommitSettings = autoCommitSettingsService.get(user.getTenantId());
        if (autoCommitSettings == null) {
            return Futures.immediateFuture(null);
        }
        AutoVersionCreateConfig autoCommitConfig = autoCommitSettings.get(entityType);
        if (autoCommitConfig == null) {
            return Futures.immediateFuture(null);
        }
        var autoCommitBranchName = autoCommitConfig.getBranch();
        if (StringUtils.isEmpty(autoCommitBranchName)) {
            autoCommitBranchName = StringUtils.isNotEmpty(repositorySettings.getDefaultBranch()) ? repositorySettings.getDefaultBranch() : "auto-commits";
        }
        ComplexVersionCreateRequest vcr = new ComplexVersionCreateRequest();
        vcr.setBranch(autoCommitBranchName);
        vcr.setVersionName("auto-commit at " + Instant.ofEpochSecond(System.currentTimeMillis() / 1000));
        vcr.setSyncStrategy(SyncStrategy.MERGE);

        EntityTypeVersionCreateConfig vcrConfig = new EntityTypeVersionCreateConfig();
        vcrConfig.setEntityIds(entityIds);
        vcr.setEntityTypes(Collections.singletonMap(entityType, vcrConfig));
        return saveEntitiesVersion(user, vcr);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：文本结果。
     */
    private String getCauseMessage(Exception e) {
        String message;
        if (e.getCause() != null && StringUtils.isNotEmpty(e.getCause().getMessage())) {
            message = e.getCause().getMessage();
        } else {
            message = e.getMessage();
        }
        return message;
    }

    /**
     * 功能：保存或创建`Result`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityType`：实体对象。
     * - `importResult`：`importResult` 参数。
     * 返回：无。
     */
    private void registerResult(EntitiesImportCtx ctx, EntityType entityType, EntityImportResult<?> importResult) {
        if (importResult.isCreated()) {
            ctx.registerResult(entityType, true);
        } else if (importResult.isUpdated() || importResult.isUpdatedRelatedEntities()) {
            ctx.registerResult(entityType, false);
        }
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `user`：`user` 参数。
     * - `request`：请求对象。
     * - `commit`：`commit` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    private void processCommitError(User user, VersionCreateRequest request, CommitGitRequest commit, Throwable e) {
        log.debug("[{}] Failed to prepare the commit: {}", user.getId(), request, e);
        cachePut(commit.getTxId(), new VersionCreationResult(e.getMessage()));
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    private void processLoadError(EntitiesImportCtx ctx, Throwable e) {
        log.debug("[{}] Failed to load the commit: {}", ctx.getRequestId(), ctx.getVersionId(), e);
        cachePut(ctx.getRequestId(), VersionLoadResult.error(EntityLoadError.runtimeError(e)));
    }

    /**
     * 功能：执行 `cachePut` 对应的处理。
     * 参数：
     * - `requestId`：请求ID。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    private void cachePut(UUID requestId, VersionCreationResult result) {
        taskCache.put(requestId, VersionControlTaskCacheEntry.newForExport(result));
    }

    /**
     * 功能：执行 `cachePut` 对应的处理。
     * 参数：
     * - `requestId`：请求ID。
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    private VersionLoadResult cachePut(UUID requestId, VersionLoadResult result) {
        log.trace("[{}] Cache put: {}", requestId, result);
        taskCache.put(requestId, VersionControlTaskCacheEntry.newForImport(result));
        return result;
    }

    /**
     * 功能：执行 `persistToCache` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    private void persistToCache(EntitiesImportCtx ctx) {
        cachePut(ctx.getRequestId(), VersionLoadResult.success(new ArrayList<>(ctx.getResults().values())));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEntitiesVersionControlService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

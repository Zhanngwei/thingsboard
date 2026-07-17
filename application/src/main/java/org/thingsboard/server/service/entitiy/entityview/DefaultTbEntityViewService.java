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
package org.thingsboard.server.service.entitiy.entityview;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isBlank;

/**
 * 中文说明：
 * 1. `DefaultTbEntityViewService` 是 ThingsBoard Application 中负责实体视图的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbEntityViewService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@AllArgsConstructor
@Slf4j
public class DefaultTbEntityViewService extends AbstractTbEntityService implements TbEntityViewService {

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    private final EntityViewService entityViewService;
    private final AttributesService attributesService;
    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    private final TelemetrySubscriptionService tsSubService;
    private final TimeseriesService tsService;

    final Map<TenantId, Map<EntityId, List<EntityView>>> localCache = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `entityView`：实体对象。
     * - `existingEntityView`：实体对象。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public EntityView save(EntityView entityView, EntityView existingEntityView, User user) throws Exception {
        ActionType actionType = entityView.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = entityView.getTenantId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.saveEntityView(entityView));
            this.updateEntityViewAttributes(tenantId, savedEntityView, existingEntityView, user);
            autoCommit(user, savedEntityView.getId());
            notificationEntityService.logEntityAction(savedEntityView.getTenantId(), savedEntityView.getId(), savedEntityView,
                    null, actionType, user);
            localCache.computeIfAbsent(savedEntityView.getTenantId(), (k) -> new ConcurrentReferenceHashMap<>()).clear();
            tbClusterService.broadcastEntityStateChangeEvent(savedEntityView.getTenantId(), savedEntityView.getId(),
                    entityView.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(user.getTenantId(), emptyId(EntityType.ENTITY_VIEW), entityView, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：更新实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `savedEntityView`：实体对象。
     * - `oldEntityView`：实体对象。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void updateEntityViewAttributes(TenantId tenantId, EntityView savedEntityView, EntityView oldEntityView, User user) throws ThingsboardException {
        List<ListenableFuture<?>> futures = new ArrayList<>();

        if (oldEntityView != null) {
            if (oldEntityView.getKeys() != null && oldEntityView.getKeys().getAttributes() != null) {
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.CLIENT_SCOPE, oldEntityView.getKeys().getAttributes().getCs(), user));
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.SERVER_SCOPE, oldEntityView.getKeys().getAttributes().getSs(), user));
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.SHARED_SCOPE, oldEntityView.getKeys().getAttributes().getSh(), user));
            }
            List<String> tsKeys = oldEntityView.getKeys() != null && oldEntityView.getKeys().getTimeseries() != null ?
                    oldEntityView.getKeys().getTimeseries() : Collections.emptyList();
            futures.add(deleteLatestFromEntityView(oldEntityView, tsKeys, user));
        }
        if (savedEntityView.getKeys() != null) {
            if (savedEntityView.getKeys().getAttributes() != null) {
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.CLIENT_SCOPE, savedEntityView.getKeys().getAttributes().getCs(), user));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SERVER_SCOPE, savedEntityView.getKeys().getAttributes().getSs(), user));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SHARED_SCOPE, savedEntityView.getKeys().getAttributes().getSh(), user));
            }
            futures.add(copyLatestFromEntityToEntityView(tenantId, savedEntityView));
        }
        for (ListenableFuture<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to copy attributes to entity view", e);
            }
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `entityView`：实体对象。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(EntityView entityView, User user) throws ThingsboardException {
        TenantId tenantId = entityView.getTenantId();
        EntityViewId entityViewId = entityView.getId();
        try {
            entityViewService.deleteEntityView(tenantId, entityViewId);
            notificationEntityService.logEntityAction(tenantId, entityViewId, entityView, entityView.getCustomerId(),
                    ActionType.DELETED, user, entityViewId.toString());

            localCache.computeIfAbsent(tenantId, (k) -> new ConcurrentReferenceHashMap<>()).clear();
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId, ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.DELETED, user, e, entityViewId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignEntityViewToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(tenantId, entityViewId, customerId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, savedEntityView.getCustomerId(),
                    actionType, user, entityViewId.toString(), customerId.toString(), customer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString(), customerId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignEntityViewFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromCustomer(tenantId, entityViewId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, customer.getId(),
                    actionType, user, savedEntityView.getId().toString(), customer.getId().toString(), customer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignEntityViewToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public EntityView assignEntityViewToPublicCustomer(TenantId tenantId, EntityViewId entityViewId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(tenantId,
                    entityViewId, publicCustomer.getId()));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, savedEntityView.getCustomerId(),
                    actionType, user, savedEntityView.getId().toString(), publicCustomer.getId().toString(), publicCustomer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignEntityViewToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityViewId`：实体视图ID。
     * - `edge`：`edge` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public EntityView assignEntityViewToEdge(TenantId tenantId, CustomerId customerId, EntityViewId entityViewId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToEdge(tenantId, entityViewId, edgeId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, customerId, actionType,
                    user, savedEntityView.getEntityId().toString(), edgeId.toString(), edge.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString(), edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignEntityViewFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityView`：实体对象。
     * - `edge`：`edge` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public EntityView unassignEntityViewFromEdge(TenantId tenantId, CustomerId customerId, EntityView entityView,
                                                 Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        EntityViewId entityViewId = entityView.getId();
        EdgeId edgeId = edge.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromEdge(tenantId, entityViewId, edgeId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, customerId, actionType,
                    user, entityViewId.toString(), edgeId.toString(), edge.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString(), edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId) {
        Map<EntityId, List<EntityView>> localCacheByTenant = localCache.computeIfAbsent(tenantId, (k) -> new ConcurrentReferenceHashMap<>());
        List<EntityView> fromLocalCache = localCacheByTenant.get(entityId);
        if (fromLocalCache != null) {
            return Futures.immediateFuture(fromLocalCache);
        }

        ListenableFuture<List<EntityView>> future = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId);

        return Futures.transform(future, (entityViewList) -> {
            localCacheByTenant.put(entityId, entityViewList);
            return entityViewList;
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `componentLifecycleMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onComponentLifecycleMsg(ComponentLifecycleMsg componentLifecycleMsg) {
        Map<EntityId, List<EntityView>> localCacheByTenant = localCache.computeIfAbsent(componentLifecycleMsg.getTenantId(), (k) -> new ConcurrentReferenceHashMap<>());
        EntityViewId entityViewId = new EntityViewId(componentLifecycleMsg.getEntityId().getId());
        deleteOldCacheValue(localCacheByTenant, entityViewId);
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED) {
            EntityView entityView = entityViewService.findEntityViewById(componentLifecycleMsg.getTenantId(), entityViewId);
            if (entityView != null) {
                localCacheByTenant.remove(entityView.getEntityId());
            }
        }
    }

    /**
     * 功能：删除或清理值。
     * 参数：
     * - `localCacheByTenant`：租户信息或租户标识。
     * - `entityViewId`：实体视图ID。
     * 返回：无。
     */
    private void deleteOldCacheValue(Map<EntityId, List<EntityView>> localCacheByTenant, EntityViewId entityViewId) {
        for (var entry : localCacheByTenant.entrySet()) {
            EntityView toDelete = null;
            for (EntityView view : entry.getValue()) {
                if (entityViewId.equals(view.getId())) {
                    toDelete = view;
                    break;
                }
            }
            if (toDelete != null) {
                entry.getValue().remove(toDelete);
                break;
            }
        }
    }

    /**
     * 功能：执行 `copyAttributesFromEntityToEntityView` 对应的处理。
     * 参数：
     * - `entityView`：实体对象。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<Void>> copyAttributesFromEntityToEntityView(EntityView entityView, String scope, Collection<String> keys, User user) throws ThingsboardException {
        EntityViewId entityId = entityView.getId();
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> getAttrFuture = attributesService.find(entityView.getTenantId(), entityView.getEntityId(), scope, keys);
            return Futures.transform(getAttrFuture, attributeKvEntries -> {
                List<AttributeKvEntry> attributes;
                if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                    attributes =
                            attributeKvEntries.stream()
                                    .filter(attributeKvEntry -> {
                                        long startTime = entityView.getStartTimeMs();
                                        long endTime = entityView.getEndTimeMs();
                                        long lastUpdateTs = attributeKvEntry.getLastUpdateTs();
                                        return startTime == 0 && endTime == 0 ||
                                                (endTime == 0 && startTime < lastUpdateTs) ||
                                                (startTime == 0 && endTime > lastUpdateTs) ||
                                                (startTime < lastUpdateTs && endTime > lastUpdateTs);
                                    }).collect(Collectors.toList());
                    tsSubService.saveAndNotify(entityView.getTenantId(), entityId, scope, attributes, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            try {
                                logAttributesUpdated(entityView.getTenantId(), user, entityId, scope, attributes, null);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            try {
                                logAttributesUpdated(entityView.getTenantId(), user, entityId, scope, attributes, t);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }
                    });
                }
                return null;
            }, MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    /**
     * 功能：执行 `copyLatestFromEntityToEntityView` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityView`：实体对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<Void>> copyLatestFromEntityToEntityView(TenantId tenantId, EntityView entityView) {
        EntityViewId entityId = entityView.getId();
        List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                entityView.getKeys().getTimeseries() : Collections.emptyList();
        long startTs = entityView.getStartTimeMs();
        long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
        ListenableFuture<List<String>> keysFuture;
        if (keys.isEmpty()) {
            keysFuture = Futures.transform(tsService.findAllLatest(tenantId,
                    entityView.getEntityId()), latest -> latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList()), MoreExecutors.directExecutor());
        } else {
            keysFuture = Futures.immediateFuture(keys);
        }
        ListenableFuture<List<TsKvEntry>> latestFuture = Futures.transformAsync(keysFuture, fetchKeys -> {
            List<ReadTsKvQuery> queries = fetchKeys.stream().filter(key -> !isBlank(key)).map(key -> new BaseReadTsKvQuery(key, startTs, endTs, 1, "DESC")).collect(Collectors.toList());
            if (!queries.isEmpty()) {
                return tsService.findAll(tenantId, entityView.getEntityId(), queries);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
        return Futures.transform(latestFuture, latestValues -> {
            if (latestValues != null && !latestValues.isEmpty()) {
                tsSubService.saveLatestAndNotify(entityView.getTenantId(), entityId, latestValues, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                });
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：删除或清理实体视图。
     * 参数：
     * - `entityView`：实体对象。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> deleteAttributesFromEntityView(EntityView entityView, String scope, List<String> keys, User user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteAndNotify(entityView.getTenantId(), entityId, scope, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logAttributesDeleted(entityView.getTenantId(), user, entityId, scope, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logAttributesDeleted(entityView.getTenantId(), user, entityId, scope, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            resultFuture.set(null);
        }
        return resultFuture;
    }

    /**
     * 功能：删除或清理实体视图。
     * 参数：
     * - `entityView`：实体对象。
     * - `keys`：键。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> deleteLatestFromEntityView(EntityView entityView, List<String> keys, User user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteLatest(entityView.getTenantId(), entityId, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            tsSubService.deleteAllLatest(entityView.getTenantId(), entityId, new FutureCallback<Collection<String>>() {
                @Override
                public void onSuccess(@Nullable Collection<String> keys) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, new ArrayList<>(keys), null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, Collections.emptyList(), t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        }
        return resultFuture;
    }

    /**
     * 功能：执行 `logAttributesUpdated` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void logAttributesUpdated(TenantId tenantId, User user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.ATTRIBUTES_UPDATED, user, toException(e), scope, attributes);
    }

    /**
     * 功能：执行 `logAttributesDeleted` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void logAttributesDeleted(TenantId tenantId, User user, EntityId entityId, String scope, List<String> keys, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.ATTRIBUTES_DELETED, user, toException(e), scope, keys);
    }

    /**
     * 功能：执行 `logTimeseriesDeleted` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void logTimeseriesDeleted(TenantId tenantId, User user, EntityId entityId, List<String> keys, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.TIMESERIES_DELETED, user, toException(e), keys);
    }

    /**
     * 功能：执行 `toException` 对应的处理。
     * 参数：
     * - `error`：错误信息。
     * 返回：处理结果。
     */
    public static Exception toException(Throwable error) {
        return error != null ? (Exception.class.isInstance(error) ? (Exception) error : new Exception(error)) : null;
    }
}

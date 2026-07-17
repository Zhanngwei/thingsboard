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
package org.thingsboard.server.service.query;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.subscription.TbAttributeSubscriptionScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DefaultEntityQueryService` 是 ThingsBoard Application 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `EntityQueryService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
@TbCoreComponent
public class DefaultEntityQueryService implements EntityQueryService {

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityService entityService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    private AlarmService alarmService;

    /**
     * 告警对象，用于描述当前业务场景。
     */
    @Value("${server.ws.max_entities_per_alarm_subscription:1000}")
    private int maxEntitiesPerAlarmSubscription;

    /**
     * 回调，负责处理对应任务或消息。
     */
    @Autowired
    private DbCallbackExecutorService dbCallbackExecutor;

    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    @Autowired
    private TimeseriesService timeseriesService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AttributesService attributesService;

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    @Override
    public long countEntitiesByQuery(SecurityUser securityUser, EntityCountQuery query) {
        return entityService.countEntitiesByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EntityData> findEntityDataByQuery(SecurityUser securityUser, EntityDataQuery query) {
        if (query.getKeyFilters() != null) {
            resolveDynamicValuesInPredicates(
                    query.getKeyFilters().stream()
                            .map(KeyFilter::getPredicate)
                            .collect(Collectors.toList()),
                    securityUser
            );
        }
        return entityService.findEntityDataByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    /**
     * 功能：执行 `resolveDynamicValuesInPredicates` 对应的处理。
     * 参数：
     * - `predicates`：数据列表。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    private void resolveDynamicValuesInPredicates(List<KeyFilterPredicate> predicates, SecurityUser user) {
        predicates.forEach(predicate -> {
            if (predicate.getType() == FilterPredicateType.COMPLEX) {
                resolveDynamicValuesInPredicates(
                        ((ComplexFilterPredicate) predicate).getPredicates(),
                        user
                );
            } else {
                setResolvedValue(user, (SimpleKeyFilterPredicate<?>) predicate);
            }
        });
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `user`：`user` 参数。
     * - `predicate`：`predicate` 参数。
     * 返回：无。
     */
    private void setResolvedValue(SecurityUser user, SimpleKeyFilterPredicate<?> predicate) {
        DynamicValue<?> dynamicValue = predicate.getValue().getDynamicValue();
        if (dynamicValue != null && dynamicValue.getResolvedValue() == null) {
            resolveDynamicValue(dynamicValue, user, predicate.getType());
        }
    }

    /**
     * 功能：执行 `resolveDynamicValue` 对应的处理。
     * 参数：
     * - `dynamicValue`：值。
     * - `user`：`user` 参数。
     * - `predicateType`：类型。
     * 返回：无。
     */
    private <T> void resolveDynamicValue(DynamicValue<T> dynamicValue, SecurityUser user, FilterPredicateType predicateType) {
        EntityId entityId;
        switch (dynamicValue.getSourceType()) {
            case CURRENT_TENANT:
                entityId = user.getTenantId();
                break;
            case CURRENT_CUSTOMER:
                entityId = user.getCustomerId();
                break;
            case CURRENT_USER:
                entityId = user.getId();
                break;
            default:
                throw new RuntimeException("Not supported operation for source type: {" + dynamicValue.getSourceType() + "}");
        }

        try {
            Optional<AttributeKvEntry> valueOpt = attributesService.find(user.getTenantId(), entityId,
                    TbAttributeSubscriptionScope.SERVER_SCOPE.name(), dynamicValue.getSourceAttribute()).get();

            if (valueOpt.isPresent()) {
                AttributeKvEntry entry = valueOpt.get();
                Object resolved = null;
                switch (predicateType) {
                    case STRING:
                        resolved = KvUtil.getStringValue(entry);
                        break;
                    case NUMERIC:
                        resolved = KvUtil.getDoubleValue(entry);
                        break;
                    case BOOLEAN:
                        resolved = KvUtil.getBoolValue(entry);
                        break;
                    case COMPLEX:
                        break;
                }

                dynamicValue.setResolvedValue((T) resolved);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmData> findAlarmDataByQuery(SecurityUser securityUser, AlarmDataQuery query) {
        EntityDataQuery entityDataQuery = this.buildEntityDataQuery(query);
        PageData<EntityData> entities = entityService.findEntityDataByQuery(securityUser.getTenantId(),
                securityUser.getCustomerId(), entityDataQuery);
        if (entities.getTotalElements() > 0) {
            LinkedHashMap<EntityId, EntityData> entitiesMap = new LinkedHashMap<>();
            for (EntityData entityData : entities.getData()) {
                entitiesMap.put(entityData.getEntityId(), entityData);
            }
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(securityUser.getTenantId(), query, entitiesMap.keySet());
            for (AlarmData alarmData : alarms.getData()) {
                EntityId entityId = alarmData.getEntityId();
                if (entityId != null) {
                    EntityData entityData = entitiesMap.get(entityId);
                    if (entityData != null) {
                        alarmData.getLatest().putAll(entityData.getLatest());
                    }
                }
            }
            return alarms;
        } else {
            return new PageData<>();
        }
    }

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    @Override
    public long countAlarmsByQuery(SecurityUser securityUser, AlarmCountQuery query) {
        return alarmService.countAlarmsByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    /**
     * 功能：构建实体。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    private EntityDataQuery buildEntityDataQuery(AlarmDataQuery query) {
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityDataSortOrder entitiesSortOrder;
        if (sortOrder == null || sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            entitiesSortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY));
        } else {
            entitiesSortOrder = sortOrder;
        }
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null, entitiesSortOrder);
        return new EntityDataQuery(query.getEntityFilter(), edpl, query.getEntityFields(), query.getLatestValues(), query.getKeyFilters());
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * - `isTimeseries`：`isTimeseries` 参数。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @Override
    public DeferredResult<ResponseEntity> getKeysByQuery(SecurityUser securityUser, TenantId tenantId, EntityDataQuery query,
                                                         boolean isTimeseries, boolean isAttributes, String attributesScope) {
        final DeferredResult<ResponseEntity> response = new DeferredResult<>();
        if (!isAttributes && !isTimeseries) {
            replyWithEmptyResponse(response);
            return response;
        }

        List<EntityId> ids = this.findEntityDataByQuery(securityUser, query).getData().stream()
                .map(EntityData::getEntityId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            replyWithEmptyResponse(response);
            return response;
        }

        Set<EntityType> types = ids.stream().map(EntityId::getEntityType).collect(Collectors.toSet());
        final ListenableFuture<List<String>> timeseriesKeysFuture;
        final ListenableFuture<List<String>> attributesKeysFuture;

        if (isTimeseries) {
            timeseriesKeysFuture = dbCallbackExecutor.submit(() -> timeseriesService.findAllKeysByEntityIds(tenantId, ids));
        } else {
            timeseriesKeysFuture = null;
        }

        if (isAttributes) {
            Map<EntityType, List<EntityId>> typesMap = ids.stream().collect(Collectors.groupingBy(EntityId::getEntityType));
            List<ListenableFuture<List<String>>> futures = new ArrayList<>(typesMap.size());
            typesMap.forEach((type, entityIds) -> futures.add(dbCallbackExecutor.submit(() -> attributesService.findAllKeysByEntityIds(tenantId, type, entityIds, attributesScope))));
            attributesKeysFuture = Futures.transform(Futures.allAsList(futures), lists -> {
                if (CollectionUtils.isEmpty(lists)) {
                    return Collections.emptyList();
                }
                return lists.stream().flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
            }, dbCallbackExecutor);
        } else {
            attributesKeysFuture = null;
        }

        if (isTimeseries && isAttributes) {
            Futures.whenAllComplete(timeseriesKeysFuture, attributesKeysFuture).run(() -> {
                try {
                    replyWithResponse(response, types, timeseriesKeysFuture.get(), attributesKeysFuture.get());
                } catch (Exception e) {
                    log.error("Failed to fetch timeseries and attributes keys!", e);
                    AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }, dbCallbackExecutor);
        } else if (isTimeseries) {
            addCallback(timeseriesKeysFuture, keys -> replyWithResponse(response, types, keys, null),
                    error -> {
                        log.error("Failed to fetch timeseries keys!", error);
                        AccessValidator.handleError(error, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        } else {
            addCallback(attributesKeysFuture, keys -> replyWithResponse(response, types, null, keys),
                    error -> {
                        log.error("Failed to fetch attributes keys!", error);
                        AccessValidator.handleError(error, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
        return response;
    }

    /**
     * 功能：执行 `replyWithResponse` 对应的处理。
     * 参数：
     * - `response`：响应对象。
     * - `types`：类型。
     * - `timeseriesKeys`：键。
     * - `attributesKeys`：键。
     * 返回：无。
     */
    private void replyWithResponse(DeferredResult<ResponseEntity> response, Set<EntityType> types, List<String> timeseriesKeys, List<String> attributesKeys) {
        ObjectNode json = JacksonUtil.newObjectNode();
        addItemsToArrayNode(json.putArray("entityTypes"), types);
        addItemsToArrayNode(json.putArray("timeseries"), timeseriesKeys);
        addItemsToArrayNode(json.putArray("attribute"), attributesKeys);
        response.setResult(new ResponseEntity<>(json, HttpStatus.OK));
    }

    /**
     * 功能：执行 `replyWithEmptyResponse` 对应的处理。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    private void replyWithEmptyResponse(DeferredResult<ResponseEntity> response) {
        replyWithResponse(response, Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * 功能：保存或创建节点实例。
     * 参数：
     * - `arrayNode`：`arrayNode` 参数。
     * - `collection`：数据列表。
     * 返回：无。
     */
    private void addItemsToArrayNode(ArrayNode arrayNode, Collection<?> collection) {
        if (!CollectionUtils.isEmpty(collection)) {
            collection.forEach(item -> arrayNode.add(item.toString()));
        }
    }

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `future`：数据列表。
     * - `success`：数据列表。
     * - `error`：错误信息。
     * 返回：无。
     */
    private void addCallback(ListenableFuture<List<String>> future, Consumer<List<String>> success, Consumer<Throwable> error) {
        Futures.addCallback(future, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(@Nullable List<String> keys) {
                success.accept(keys);
            }

            @Override
            public void onFailure(Throwable t) {
                error.accept(t);
            }
        }, dbCallbackExecutor);
    }

}

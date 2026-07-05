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
package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`TbAlarmDataSubCtx` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@ToString(callSuper = true)
public class TbAlarmDataSubCtx extends TbAbstractDataSubCtx<AlarmDataQuery> {

    /**
     * 告警，提供当前类调用的业务操作。
     */
    private final AlarmService alarmService;
    /**
     * `entitiesMap`映射关系，用于按键查找对应值。
     */
    @Getter
    private final LinkedHashMap<EntityId, EntityData> entitiesMap;
    /**
     * `alarmsMap`映射关系，用于按键查找对应值。
     */
    @Getter
    private final HashMap<AlarmId, AlarmData> alarmsMap;

    /**
     * 告警对象，用于描述当前业务场景。
     */
    private final int maxEntitiesPerAlarmSubscription;

    /**
     * 告警对象，用于描述当前业务场景。
     */
    private final int maxAlarmQueriesPerRefreshInterval;

    /**
     * `alarms` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Setter
    private PageData<AlarmData> alarms;
    /**
     * 是否满足`tooManyEntities`条件。
     */
    @Getter
    @Setter
    private boolean tooManyEntities;

    /**
     * 告警对象，用于描述当前业务场景。
     */
    private int alarmInvocationAttempts;

    /**
     * 功能：创建 `TbAlarmDataSubCtx` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `wsService`：服务对象。
     * - `entityService`：服务对象。
     * - `localSubscriptionService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbAlarmDataSubCtx(String serviceId, WebSocketService wsService,
                             EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                             AttributesService attributesService, SubscriptionServiceStatistics stats, AlarmService alarmService,
                             WebSocketSessionRef sessionRef, int cmdId,
                             int maxEntitiesPerAlarmSubscription, int maxAlarmQueriesPerRefreshInterval) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.maxEntitiesPerAlarmSubscription = maxEntitiesPerAlarmSubscription;
        this.maxAlarmQueriesPerRefreshInterval = maxAlarmQueriesPerRefreshInterval;
        this.alarmService = alarmService;
        this.entitiesMap = new LinkedHashMap<>();
        this.alarmsMap = new HashMap<>();
    }

    /**
     * 功能：获取`Alarms`。
     * 参数：无。
     * 返回：无。
     */
    public void fetchAlarms() {
        alarmInvocationAttempts++;
        log.trace("[{}] Fetching alarms: {}", cmdId, alarmInvocationAttempts);
        if (alarmInvocationAttempts <= maxAlarmQueriesPerRefreshInterval) {
            doFetchAlarms();
        } else {
            log.trace("[{}] Ignore alarm fetch due to rate limit: [{}] of maximum [{}]", cmdId, alarmInvocationAttempts, maxAlarmQueriesPerRefreshInterval);
        }
    }

    /**
     * 功能：执行 `doFetchAlarms` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void doFetchAlarms() {
        AlarmDataUpdate update;
        if (!entitiesMap.isEmpty()) {
            long start = System.currentTimeMillis();
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(getTenantId(), query, getOrderedEntityIds());
            long end = System.currentTimeMillis();
            stats.getAlarmQueryInvocationCnt().incrementAndGet();
            stats.getAlarmQueryTimeSpent().addAndGet(end - start);
            alarms = setAndMergeAlarmsData(alarms);
            update = new AlarmDataUpdate(cmdId, alarms, null, maxEntitiesPerAlarmSubscription, data.getTotalElements());
        } else {
            update = new AlarmDataUpdate(cmdId, new PageData<>(), null, maxEntitiesPerAlarmSubscription, data.getTotalElements());
        }
        sendWsMsg(update);
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：无。
     */
    public void fetchData() {
        resetInvocationCounter();
        log.trace("[{}] Fetching data: {}", cmdId, alarmInvocationAttempts);
        super.fetchData();
        entitiesMap.clear();
        tooManyEntities = data.hasNext();
        for (EntityData entityData : data.getData()) {
            entitiesMap.put(entityData.getEntityId(), entityData);
        }
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Collection<EntityId> getOrderedEntityIds() {
        return entitiesMap.keySet();
    }

    /**
     * 功能：更新数据。
     * 参数：
     * - `alarms`：`alarms` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<AlarmData> setAndMergeAlarmsData(PageData<AlarmData> alarms) {
        this.alarms = alarms;
        for (AlarmData alarmData : alarms.getData()) {
            EntityId entityId = alarmData.getEntityId();
            if (entityId != null) {
                EntityData entityData = entitiesMap.get(entityId);
                if (entityData != null) {
                    alarmData.getLatest().putAll(entityData.getLatest());
                }
            }
        }
        alarmsMap.clear();
        alarmsMap.putAll(alarms.getData().stream().collect(Collectors.toMap(AlarmData::getId, Function.identity(), (a, b) -> a)));
        return this.alarms;
    }

    /**
     * 功能：保存或创建`Latest Values Subscriptions`。
     * 参数：
     * - `keys`：键。
     * 返回：无。
     */
    @Override
    public void createLatestValuesSubscriptions(List<EntityKey> keys) {
        super.createLatestValuesSubscriptions(keys);
        createAlarmSubscriptions();
    }

    /**
     * 功能：保存或创建告警。
     * 参数：无。
     * 返回：无。
     */
    public void createAlarmSubscriptions() {
        AlarmDataPageLink pageLink = query.getPageLink();
        long startTs = System.currentTimeMillis() - pageLink.getTimeWindow();
        for (EntityData entityData : entitiesMap.values()) {
            createAlarmSubscriptionForEntity(pageLink, startTs, entityData);
        }
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `startTs`：时间戳。
     * - `entityData`：待处理数据。
     * 返回：无。
     */
    private void createAlarmSubscriptionForEntity(AlarmDataPageLink pageLink, long startTs, EntityData entityData) {
        int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
        subToEntityIdMap.put(subIdx, entityData.getEntityId());
        log.trace("[{}][{}][{}] Creating alarms subscription for [{}] with query: {}", serviceId, cmdId, subIdx, entityData.getEntityId(), pageLink);
        TbAlarmsSubscription subscription = TbAlarmsSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityData.getEntityId())
                .updateProcessor((sub, update) -> sendWsMsg(sub.getSessionId(), update))
                .ts(startTs)
                .build();
        localSubscriptionService.addSubscription(subscription);
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `sessionId`：会话ID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * - `keyType`：类型。
     * - `resultToLatestValues`：值。
     * 返回：无。
     */
    @Override
    void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType, boolean resultToLatestValues) {
        EntityId entityId = subToEntityIdMap.get(subscriptionUpdate.getSubscriptionId());
        if (entityId != null) {
            Map<String, TsValue> latestUpdate = new HashMap<>();
            subscriptionUpdate.getData().forEach((k, v) -> {
                Object[] data = (Object[]) v.get(0);
                latestUpdate.put(k, new TsValue((Long) data[0], (String) data[1]));
            });
            EntityData entityData = entitiesMap.get(entityId);
            entityData.getLatest().computeIfAbsent(keyType, tmp -> new HashMap<>()).putAll(latestUpdate);
            log.trace("[{}][{}][{}][{}] Received subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
            List<AlarmData> update = alarmsMap.values().stream().filter(alarm -> entityId.equals(alarm.getEntityId())).map(alarm -> {
                alarm.getLatest().computeIfAbsent(keyType, tmp -> new HashMap<>()).putAll(latestUpdate);
                return alarm;
            }).collect(Collectors.toList());
            if (!update.isEmpty()) {
                sendWsMsg(new AlarmDataUpdate(cmdId, null, update, maxEntitiesPerAlarmSubscription, data.getTotalElements()));
            }
        } else {
            log.trace("[{}][{}][{}][{}] Received stale subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
        }
    }

    /**
     * 功能：获取`Current Aggregation`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Aggregation getCurrentAggregation() {
        return Aggregation.NONE;
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `sessionId`：会话ID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * 返回：无。
     */
    private void sendWsMsg(String sessionId, AlarmSubscriptionUpdate subscriptionUpdate) {
        Alarm alarm = subscriptionUpdate.getAlarm();
        AlarmId alarmId = alarm.getId();
        if (subscriptionUpdate.isAlarmDeleted()) {
            Alarm deleted = alarmsMap.remove(alarmId);
            if (deleted != null) {
                fetchAlarms();
            }
        } else {
            AlarmData current = alarmsMap.get(alarmId);
            boolean onCurrentPage = current != null;
            boolean matchesFilter = filter(alarm);
            if (onCurrentPage) {
                if (matchesFilter) {
                    AlarmData updated = new AlarmData(subscriptionUpdate.getAlarm(), current);
                    alarmsMap.put(alarmId, updated);
                    sendWsMsg(new AlarmDataUpdate(cmdId, null, Collections.singletonList(updated), maxEntitiesPerAlarmSubscription, data.getTotalElements()));
                } else {
                    fetchAlarms();
                }
            } else if (matchesFilter && query.getPageLink().getPage() == 0) {
                fetchAlarms();
            }
        }
    }

    /**
     * 功能：删除或清理`Old Alarms`。
     * 参数：无。
     * 返回：无。
     */
    public void cleanupOldAlarms() {
        long expTime = System.currentTimeMillis() - query.getPageLink().getTimeWindow();
        boolean shouldRefresh = false;
        for (AlarmData alarmData : alarms.getData()) {
            if (alarmData.getCreatedTime() < expTime) {
                shouldRefresh = true;
                break;
            }
        }
        if (shouldRefresh) {
            doFetchAlarms();
        }
    }

    /**
     * 功能：执行 `filter` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：判断结果。
     */
    private boolean filter(Alarm alarm) {
        AlarmDataPageLink filter = query.getPageLink();
        long startTs = System.currentTimeMillis() - filter.getTimeWindow();
        if (alarm.getCreatedTime() < startTs) {
            //Skip update that does not match time window.
            return false;
        }
        if (filter.getTypeList() != null && !filter.getTypeList().isEmpty() && !filter.getTypeList().contains(alarm.getType())) {
            return false;
        }
        if (filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
            if (!filter.getSeverityList().contains(alarm.getSeverity())) {
                return false;
            }
        }
        if (filter.getStatusList() != null && !filter.getStatusList().isEmpty()) {
            boolean matches = false;
            for (AlarmSearchStatus status : filter.getStatusList()) {
                switch (status) {
                    case ANY:
                        matches = true;
                        break;
                    case ACK:
                        matches = alarm.isAcknowledged();
                        break;
                    case UNACK:
                        matches = !alarm.isAcknowledged();
                        break;
                    case CLEARED:
                        matches = alarm.isCleared();
                        break;
                    case ACTIVE:
                        matches = !alarm.isCleared();
                        break;
                }
                if (matches) {
                    break;
                }
            }
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    /**
     * 功能：校验计数器。
     * 参数：无。
     * 返回：无。
     */
    public synchronized void checkAndResetInvocationCounter() {
        boolean fetchNeeded = this.alarmInvocationAttempts > maxAlarmQueriesPerRefreshInterval;
        resetInvocationCounter();
        if (fetchNeeded) {
            fetchAlarms();
        } else {
            cleanupOldAlarms();
        }
    }

    /**
     * 功能：执行 `doUpdate` 对应的处理。
     * 参数：
     * - `newDataMap`：待处理数据。
     * 返回：无。
     */
    @Override
    protected synchronized void doUpdate(Map<EntityId, EntityData> newDataMap) {
        resetInvocationCounter();
        entitiesMap.clear();
        tooManyEntities = data.hasNext();
        for (EntityData entityData : data.getData()) {
            entitiesMap.put(entityData.getEntityId(), entityData);
        }
        fetchAlarms();
        List<Integer> subIdsToCancel = new ArrayList<>();
        List<TbSubscription> subsToAdd = new ArrayList<>();
        Set<EntityId> currentSubs = new HashSet<>();
        subToEntityIdMap.forEach((subId, entityId) -> {
            if (!newDataMap.containsKey(entityId)) {
                subIdsToCancel.add(subId);
            } else {
                currentSubs.add(entityId);
            }
        });
        log.trace("[{}][{}] Subscriptions that are invalid: {}", sessionRef.getSessionId(), cmdId, subIdsToCancel);
        subIdsToCancel.forEach(subToEntityIdMap::remove);
        List<EntityData> newSubsList = newDataMap.entrySet().stream().filter(entry -> !currentSubs.contains(entry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        if (!newSubsList.isEmpty()) {
            List<EntityKey> keys = query.getLatestValues();
            if (keys != null && !keys.isEmpty()) {
                Map<EntityKeyType, List<EntityKey>> keysByType = getEntityKeyByTypeMap(keys);
                newSubsList.forEach(
                        entity -> {
                            log.trace("[{}][{}] Found new subscription for entity: {}", sessionRef.getSessionId(), cmdId, entity.getEntityId());
                            subsToAdd.addAll(addSubscriptions(entity, keysByType, true, 0, 0));
                        }
                );
            }
            long startTs = System.currentTimeMillis() - query.getPageLink().getTimeWindow();
            newSubsList.forEach(entity -> createAlarmSubscriptionForEntity(query.getPageLink(), startTs, entity));
        }
        subIdsToCancel.forEach(subId -> localSubscriptionService.cancelSubscription(getSessionId(), subId));
        subsToAdd.forEach(localSubscriptionService::addSubscription);
    }

    /**
     * 功能：执行 `resetInvocationCounter` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void resetInvocationCounter() {
        alarmInvocationAttempts = 0;
    }

    /**
     * 功能：构建实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected EntityDataQuery buildEntityDataQuery() {
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
}

/*
 * 本类总结：
 * 1. 核心职责：`TbAlarmDataSubCtx` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

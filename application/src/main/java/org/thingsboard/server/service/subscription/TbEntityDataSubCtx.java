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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.TimeSeriesCmd;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`TbEntityDataSubCtx` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public class TbEntityDataSubCtx extends TbAbstractDataSubCtx<EntityDataQuery> {

    /**
     * 是否满足数据条件。
     */
    @Getter
    @Setter
    private volatile boolean initialDataSent;
    private TimeSeriesCmd curTsCmd;
    /**
     * 最新数据订阅命令，保存当前处理得到的具体内容。
     */
    private LatestValueCmd latestValueCmd;
    /**
     * 订阅，保存当前步骤读取或计算得到的内容。
     */
    @Getter
    private final int maxEntitiesPerDataSubscription;
    private Map<EntityId, Map<String, TsValue>> latestTsEntityData;

    /**
     * 功能：创建 `TbEntityDataSubCtx` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `wsService`：服务对象。
     * - `entityService`：服务对象。
     * - `localSubscriptionService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbEntityDataSubCtx(String serviceId, WebSocketService wsService, EntityService entityService,
                              TbLocalSubscriptionService localSubscriptionService, AttributesService attributesService,
                              SubscriptionServiceStatistics stats, WebSocketSessionRef sessionRef, int cmdId, int maxEntitiesPerDataSubscription) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.maxEntitiesPerDataSubscription = maxEntitiesPerDataSubscription;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void fetchData() {
        super.fetchData();
        this.updateLatestTsData(this.data);
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
    protected void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType, boolean resultToLatestValues) {
        EntityId entityId = subToEntityIdMap.get(subscriptionUpdate.getSubscriptionId());
        if (entityId != null) {
            log.trace("[{}][{}][{}][{}] Received subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
            if (resultToLatestValues) {
                sendLatestWsMsg(entityId, sessionId, subscriptionUpdate, keyType);
            } else {
                sendTsWsMsg(entityId, sessionId, subscriptionUpdate, keyType);
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
        return (this.curTsCmd == null || this.curTsCmd.getAgg() == null) ? Aggregation.NONE : this.curTsCmd.getAgg();
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `entityId`：实体IDID。
     * - `sessionId`：会话ID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * - `keyType`：类型。
     * 返回：无。
     */
    private void sendLatestWsMsg(EntityId entityId, String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        Map<String, TsValue> latestUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            latestUpdate.put(k, new TsValue((Long) data[0], (String) data[1]));
        });
        EntityData entityData = getDataForEntity(entityId);
        if (entityData != null && entityData.getLatest() != null) {
            Map<String, TsValue> latestCtxValues = entityData.getLatest().get(keyType);
            log.trace("[{}][{}][{}] Going to compare update with {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), latestCtxValues);
            if (latestCtxValues != null) {
                latestCtxValues.forEach((k, v) -> {
                    TsValue update = latestUpdate.get(k);
                    if (update != null) {
                        //Ignore notifications about deleted keys
                        if (!(update.getTs() == 0 && (update.getValue() == null || update.getValue().isEmpty()))) {
                            if (update.getTs() < v.getTs()) {
                                log.trace("[{}][{}][{}] Removed stale update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                                latestUpdate.remove(k);
                            } else if ((update.getTs() == v.getTs() && update.getValue().equals(v.getValue()))) {
                                log.trace("[{}][{}][{}] Removed duplicate update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                                latestUpdate.remove(k);
                            }
                        } else {
                            log.trace("[{}][{}][{}] Received deleted notification for: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k);
                        }
                    }
                });
                //Setting new values
                latestUpdate.forEach(latestCtxValues::put);
            }
        }
        if (!latestUpdate.isEmpty()) {
            Map<EntityKeyType, Map<String, TsValue>> latestMap = Collections.singletonMap(keyType, latestUpdate);
            entityData = new EntityData(entityId, latestMap, null);
            sendWsMsg(new EntityDataUpdate(cmdId, null, Collections.singletonList(entityData), maxEntitiesPerDataSubscription));
        }
    }

    /**
     * 功能：发送或提交时间戳。
     * 参数：
     * - `entityId`：实体IDID。
     * - `sessionId`：会话ID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * - `keyType`：类型。
     * 返回：无。
     */
    private void sendTsWsMsg(EntityId entityId, String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        Map<String, List<TsValue>> tsUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            tsUpdate.computeIfAbsent(k, key -> new ArrayList<>()).add(new TsValue((Long) data[0], (String) data[1]));
        });
        Map<String, TsValue> latestCtxValues = getLatestTsValuesForEntity(entityId);
        log.trace("[{}][{}][{}] Going to compare update with {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), latestCtxValues);
        if (latestCtxValues != null) {
            latestCtxValues.forEach((k, v) -> {
                List<TsValue> updateList = tsUpdate.get(k);
                if (updateList != null) {
                    for (TsValue update : new ArrayList<>(updateList)) {
                        if (update.getTs() < v.getTs()) {
                            log.trace("[{}][{}][{}] Removed stale update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            // Looks like this is redundant feature and our UI is ready to merge the updates.
                            //updateList.remove(update);
                        } else if ((update.getTs() == v.getTs() && update.getValue().equals(v.getValue()))) {
                            log.trace("[{}][{}][{}] Removed duplicate update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            updateList.remove(update);
                        }
                        if (updateList.isEmpty()) {
                            tsUpdate.remove(k);
                        }
                    }
                }
            });
            //Setting new values
            tsUpdate.forEach((k, v) -> {
                Optional<TsValue> maxValue = v.stream().max(Comparator.comparingLong(TsValue::getTs));
                maxValue.ifPresent(max -> latestCtxValues.put(k, max));
            });
        }
        if (!tsUpdate.isEmpty()) {
            Map<String, TsValue[]> tsMap = new HashMap<>();
            tsUpdate.forEach((key, tsValue) -> tsMap.put(key, tsValue.toArray(new TsValue[tsValue.size()])));
            EntityData entityData = new EntityData(entityId, null, tsMap);
            sendWsMsg(new EntityDataUpdate(cmdId, null, Collections.singletonList(entityData), maxEntitiesPerDataSubscription));
        }
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private EntityData getDataForEntity(EntityId entityId) {
        return data.getData().stream().filter(item -> item.getEntityId().equals(entityId)).findFirst().orElse(null);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private Map<String, TsValue> getLatestTsValuesForEntity(EntityId entityId) {
        return latestTsEntityData.get(entityId);
    }

    /**
     * 功能：更新时间戳。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    private void updateLatestTsData(PageData<EntityData> data) {
        latestTsEntityData = new HashMap<>();
        data.getData().stream().forEach(entityData -> {
            Map<String, TsValue> latestTsMap = new HashMap<>();
            latestTsEntityData.put(entityData.getEntityId(), latestTsMap);
            if (entityData.getLatest() != null) {
                Map<String, TsValue> latestTsValues = entityData.getLatest().get(EntityKeyType.TIME_SERIES);
                if (latestTsValues != null) {
                    latestTsValues.forEach(latestTsMap::put);
                }
            }
        });
    }

    /**
     * 功能：执行 `doUpdate` 对应的处理。
     * 参数：
     * - `newDataMap`：待处理数据。
     * 返回：无。
     */
    @Override
    public synchronized void doUpdate(Map<EntityId, EntityData> newDataMap) {
        this.updateLatestTsData(this.data);
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
            // NOTE: We ignore the TS subscriptions for new entities here, because widgets will re-init it's content and will create new subscriptions.
            if (curTsCmd == null && latestValueCmd != null) {
                List<EntityKey> keys = latestValueCmd.getKeys();
                if (keys != null && !keys.isEmpty()) {
                    Map<EntityKeyType, List<EntityKey>> keysByType = getEntityKeyByTypeMap(keys);
                    newSubsList.forEach(
                            entity -> {
                                log.trace("[{}][{}] Found new subscription for entity: {}", sessionRef.getSessionId(), cmdId, entity.getEntityId());
                                subsToAdd.addAll(addSubscriptions(entity, keysByType, true, 0, 0));
                            }
                    );
                }
            }
        }
        subIdsToCancel.forEach(subId -> localSubscriptionService.cancelSubscription(getSessionId(), subId));
        subsToAdd.forEach(localSubscriptionService::addSubscription);
        sendWsMsg(new EntityDataUpdate(cmdId, data, null, maxEntitiesPerDataSubscription));
    }

    /**
     * 功能：更新`Current Cmd`。
     * 参数：
     * - `cmd`：`cmd` 参数。
     * 返回：无。
     */
    public void setCurrentCmd(EntityDataCmd cmd) {
        curTsCmd = cmd.getTsCmd();
        latestValueCmd = cmd.getLatestCmd();
    }

    /**
     * 功能：构建实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected EntityDataQuery buildEntityDataQuery() {
        return query;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbEntityDataSubCtx` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AbstractDataQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `TbAbstractDataSubCtx` 是 ThingsBoard Application 中围绕 `Tb Sub Ctx` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `AbstractDataQuery`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public abstract class TbAbstractDataSubCtx<T extends AbstractDataQuery<? extends EntityDataPageLink>> extends TbAbstractSubCtx<T> {

    /**
     * 实体ID映射关系，用于按键查找对应值。
     */
    protected final Map<Integer, EntityId> subToEntityIdMap;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Getter
    protected PageData<EntityData> data;

    /**
     * 功能：创建 `TbAbstractDataSubCtx` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `wsService`：服务对象。
     * - `entityService`：服务对象。
     * - `localSubscriptionService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbAbstractDataSubCtx(String serviceId, WebSocketService wsService,
                                EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                                AttributesService attributesService, SubscriptionServiceStatistics stats,
                                WebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.subToEntityIdMap = new ConcurrentHashMap<>();
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void fetchData() {
        this.data = findEntityData();
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    protected PageData<EntityData> findEntityData() {
        PageData<EntityData> result = entityService.findEntityDataByQuery(getTenantId(), getCustomerId(), buildEntityDataQuery());
        if (log.isTraceEnabled()) {
            result.getData().forEach(ed -> {
                log.trace("[{}][{}] EntityData: {}", getSessionId(), getCmdId(), ed);
            });
        }
        return result;
    }

    /**
     * 功能：判断`Dynamic`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isDynamic() {
        return query != null && query.getPageLink().isDynamic();
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected synchronized void update() {
        PageData<EntityData> newData = findEntityData();
        Map<EntityId, EntityData> oldDataMap;
        if (data != null && !data.getData().isEmpty()) {
            oldDataMap = data.getData().stream().collect(Collectors.toMap(EntityData::getEntityId, Function.identity(), (a, b) -> a));
        } else {
            oldDataMap = Collections.emptyMap();
        }
        Map<EntityId, EntityData> newDataMap = newData.getData().stream().collect(Collectors.toMap(EntityData::getEntityId, Function.identity(), (a, b) -> a));
        if (oldDataMap.size() == newDataMap.size() && oldDataMap.keySet().equals(newDataMap.keySet())) {
            log.trace("[{}][{}] No updates to entity data found", sessionRef.getSessionId(), cmdId);
        } else {
            this.data = newData;
            doUpdate(newDataMap);
        }
    }

    /**
     * 功能：执行 `doUpdate` 对应的处理。
     * 参数：
     * - `newDataMap`：待处理数据。
     * 返回：无。
     */
    protected abstract void doUpdate(Map<EntityId, EntityData> newDataMap);

    /**
     * 功能：构建实体。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract EntityDataQuery buildEntityDataQuery();

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<EntityData> getEntitiesData() {
        return data.getData();
    }

    /**
     * 功能：删除或清理`Subscriptions`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void clearSubscriptions() {
        clearEntitySubscriptions();
        super.clearSubscriptions();
    }

    /**
     * 功能：删除或清理实体。
     * 参数：无。
     * 返回：无。
     */
    public void clearEntitySubscriptions() {
        if (subToEntityIdMap != null) {
            for (Integer subId : subToEntityIdMap.keySet()) {
                localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), subId);
            }
            subToEntityIdMap.clear();
        }
    }

    /**
     * 功能：保存或创建`Latest Values Subscriptions`。
     * 参数：
     * - `keys`：键。
     * 返回：无。
     */
    public void createLatestValuesSubscriptions(List<EntityKey> keys) {
        createSubscriptions(keys, true, 0, 0);
    }

    /**
     * 功能：保存或创建时序数据。
     * 参数：
     * - `entityKeyStates`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：无。
     */
    public void createTimeSeriesSubscriptions(Map<EntityData, Map<String, Long>> entityKeyStates, long startTs, long endTs) {
        createTimeSeriesSubscriptions(entityKeyStates, startTs, endTs, false);
    }

    /**
     * 功能：保存或创建时序数据。
     * 参数：
     * - `entityKeyStates`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - `resultToLatestValues`：值。
     * 返回：无。
     */
    public void createTimeSeriesSubscriptions(Map<EntityData, Map<String, Long>> entityKeyStates, long startTs, long endTs, boolean resultToLatestValues) {
        entityKeyStates.forEach((entityData, keyStates) -> {
            int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
            subToEntityIdMap.put(subIdx, entityData.getEntityId());
            localSubscriptionService.addSubscription(
                    createTsSub(entityData, subIdx, false, startTs, endTs, keyStates, resultToLatestValues));
        });
    }

    /**
     * 功能：保存或创建`Subscriptions`。
     * 参数：
     * - `keys`：键。
     * - `latestValues`：值。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：无。
     */
    private void createSubscriptions(List<EntityKey> keys, boolean latestValues, long startTs, long endTs) {
        Map<EntityKeyType, List<EntityKey>> keysByType = getEntityKeyByTypeMap(keys);
        for (EntityData entityData : data.getData()) {
            List<TbSubscription> entitySubscriptions = addSubscriptions(entityData, keysByType, latestValues, startTs, endTs);
            entitySubscriptions.forEach(localSubscriptionService::addSubscription);
        }
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    protected Map<EntityKeyType, List<EntityKey>> getEntityKeyByTypeMap(List<EntityKey> keys) {
        Map<EntityKeyType, List<EntityKey>> keysByType = new HashMap<>();
        keys.forEach(key -> keysByType.computeIfAbsent(key.getType(), k -> new ArrayList<>()).add(key));
        return keysByType;
    }

    /**
     * 功能：保存或创建`Subscriptions`。
     * 参数：
     * - `entityData`：待处理数据。
     * - `keysByType`：类型。
     * - `latestValues`：值。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    protected List<TbSubscription> addSubscriptions(EntityData entityData, Map<EntityKeyType, List<EntityKey>> keysByType, boolean latestValues, long startTs, long endTs) {
        List<TbSubscription> subscriptionList = new ArrayList<>();
        keysByType.forEach((keysType, keysList) -> {
            int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
            subToEntityIdMap.put(subIdx, entityData.getEntityId());
            switch (keysType) {
                case TIME_SERIES:
                    subscriptionList.add(createTsSub(entityData, subIdx, keysList, latestValues, startTs, endTs));
                    break;
                case CLIENT_ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.CLIENT_SCOPE, keysList));
                    break;
                case SHARED_ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.SHARED_SCOPE, keysList));
                    break;
                case SERVER_ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.SERVER_SCOPE, keysList));
                    break;
                case ATTRIBUTE:
                    subscriptionList.add(createAttrSub(entityData, subIdx, keysType, TbAttributeSubscriptionScope.ANY_SCOPE, keysList));
                    break;
            }
        });
        return subscriptionList;
    }

    /**
     * 功能：保存或创建`Attr Sub`。
     * 参数：
     * - `entityData`：待处理数据。
     * - `subIdx`：`subIdx` 参数。
     * - `keysType`：类型。
     * - `scope`：`scope` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TbSubscription createAttrSub(EntityData entityData, int subIdx, EntityKeyType keysType, TbAttributeSubscriptionScope scope, List<EntityKey> subKeys) {
        Map<String, Long> keyStates = buildKeyStats(entityData, keysType, subKeys, true);
        log.trace("[{}][{}][{}] Creating attributes subscription for [{}] with keys: {}", serviceId, cmdId, subIdx, entityData.getEntityId(), keyStates);
        return TbAttributeSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityData.getEntityId())
                .updateProcessor((sub, subscriptionUpdate) -> sendWsMsg(sub.getSessionId(), subscriptionUpdate, keysType))
                .queryTs(createdTime)
                .allKeys(false)
                .keyStates(keyStates)
                .scope(scope)
                .build();
    }

    /**
     * 功能：保存或创建时间戳。
     * 参数：
     * - `entityData`：待处理数据。
     * - `subIdx`：`subIdx` 参数。
     * - `subKeys`：键。
     * - `latestValues`：值。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TbSubscription createTsSub(EntityData entityData, int subIdx, List<EntityKey> subKeys, boolean latestValues, long startTs, long endTs) {
        Map<String, Long> keyStates = buildKeyStats(entityData, EntityKeyType.TIME_SERIES, subKeys, latestValues);
        if (!latestValues && entityData.getTimeseries() != null) {
            entityData.getTimeseries().forEach((k, v) -> {
                long ts = Arrays.stream(v).map(TsValue::getTs).max(Long::compareTo).orElse(0L);
                log.trace("[{}][{}] Updating key: {} with ts: {}", serviceId, cmdId, k, ts);
                if (!Aggregation.NONE.equals(getCurrentAggregation()) && ts < endTs) {
                    ts = endTs;
                }
                keyStates.put(k, ts);
            });
        }
        return createTsSub(entityData, subIdx, latestValues, startTs, endTs, keyStates);
    }

    /**
     * 功能：保存或创建时间戳。
     * 参数：
     * - `entityData`：待处理数据。
     * - `subIdx`：`subIdx` 参数。
     * - `latestValues`：值。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TbTimeSeriesSubscription createTsSub(EntityData entityData, int subIdx, boolean latestValues, long startTs, long endTs, Map<String, Long> keyStates) {
        return createTsSub(entityData, subIdx, latestValues, startTs, endTs, keyStates, latestValues);
    }

    /**
     * 功能：保存或创建时间戳。
     * 参数：
     * - `entityData`：待处理数据。
     * - `subIdx`：`subIdx` 参数。
     * - `latestValues`：值。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TbTimeSeriesSubscription createTsSub(EntityData entityData, int subIdx, boolean latestValues, long startTs, long endTs, Map<String, Long> keyStates, boolean resultToLatestValues) {
        log.trace("[{}][{}][{}] Creating time-series subscription for [{}] with keys: {}", serviceId, cmdId, subIdx, entityData.getEntityId(), keyStates);
        return TbTimeSeriesSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityData.getEntityId())
                .updateProcessor((sub, subscriptionUpdate) -> sendWsMsg(sub.getSessionId(), subscriptionUpdate, EntityKeyType.TIME_SERIES, resultToLatestValues))
                .queryTs(createdTime)
                .allKeys(false)
                .keyStates(keyStates)
                .latestValues(latestValues)
                .startTime(startTs)
                .endTime(endTs)
                .build();
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `sessionId`：会话ID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * - `keyType`：类型。
     * 返回：无。
     */
    private void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        sendWsMsg(sessionId, subscriptionUpdate, keyType, true);
    }

    /**
     * 功能：构建键。
     * 参数：
     * - `entityData`：待处理数据。
     * - `keysType`：类型。
     * - `subKeys`：键。
     * - `latestValues`：值。
     * 返回：处理结果。
     */
    private Map<String, Long> buildKeyStats(EntityData entityData, EntityKeyType keysType, List<EntityKey> subKeys, boolean latestValues) {
        Map<String, Long> keyStates = new HashMap<>();
        subKeys.forEach(key -> keyStates.put(key.getKey(), 0L));
        if (latestValues && entityData.getLatest() != null) {
            Map<String, TsValue> currentValues = entityData.getLatest().get(keysType);
            if (currentValues != null) {
                currentValues.forEach((k, v) -> {
                    if (subKeys.contains(new EntityKey(keysType, k))) {
                        log.trace("[{}][{}] Updating key: {} with ts: {}", serviceId, cmdId, k, v.getTs());
                        keyStates.put(k, v.getTs());
                    }
                });
            }
        }
        return keyStates;
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
    abstract void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType, boolean resultToLatestValues);

    /**
     * 功能：获取`Current Aggregation`。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract Aggregation getCurrentAggregation();
}

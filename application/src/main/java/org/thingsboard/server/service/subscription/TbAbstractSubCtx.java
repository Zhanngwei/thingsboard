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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`TbAbstractSubCtx` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Data
public abstract class TbAbstractSubCtx<T extends EntityCountQuery> {

    @Getter
    protected final Lock wsLock = new ReentrantLock(true);
    /**
     * 服务ID，用于定位对应业务对象。
     */
    protected final String serviceId;
    protected final SubscriptionServiceStatistics stats;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final WebSocketService wsService;
    protected final EntityService entityService;
    /**
     * 订阅，提供当前类调用的业务操作。
     */
    protected final TbLocalSubscriptionService localSubscriptionService;
    protected final AttributesService attributesService;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    protected final WebSocketSessionRef sessionRef;
    protected final int cmdId;
    /**
     * 键集合，用于去重保存或快速判断对象是否存在。
     */
    protected final Set<Integer> subToDynamicValueKeySet;
    /**
     * `dynamicValues`列表，用于保存一组待处理对象。
     */
    @Getter
    protected final Map<DynamicValueKey, List<DynamicValue>> dynamicValues;
    /**
     * 查询条件，表示当前对象的对应属性。
     */
    @Getter
    @Setter
    protected T query;
    /**
     * `refreshTask` 字段，保存当前对象的对应属性。
     */
    @Setter
    protected volatile ScheduledFuture<?> refreshTask;
    protected volatile boolean stopped;
    /**
     * 创建时间，用于记录当前对象首次生成的时间。
     */
    @Getter
    protected long createdTime;

    /**
     * 功能：创建 `TbAbstractSubCtx` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `wsService`：服务对象。
     * - `entityService`：服务对象。
     * - `localSubscriptionService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbAbstractSubCtx(String serviceId, WebSocketService wsService,
                            EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                            AttributesService attributesService, SubscriptionServiceStatistics stats,
                            WebSocketSessionRef sessionRef, int cmdId) {
        this.createdTime = System.currentTimeMillis();
        this.serviceId = serviceId;
        this.wsService = wsService;
        this.entityService = entityService;
        this.localSubscriptionService = localSubscriptionService;
        this.attributesService = attributesService;
        this.stats = stats;
        this.sessionRef = sessionRef;
        this.cmdId = cmdId;
        this.subToDynamicValueKeySet = ConcurrentHashMap.newKeySet();
        this.dynamicValues = new ConcurrentHashMap<>();
    }

    /**
     * 功能：更新查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：无。
     */
    public void setAndResolveQuery(T query) {
        dynamicValues.clear();
        this.query = query;
        if (query != null && query.getKeyFilters() != null) {
            for (KeyFilter filter : query.getKeyFilters()) {
                registerDynamicValues(filter.getPredicate());
            }
        }
        resolve(getTenantId(), getCustomerId(), getUserId());
    }

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `userId`：用户ID。
     * 返回：无。
     */
    public void resolve(TenantId tenantId, CustomerId customerId, UserId userId) {
        List<ListenableFuture<DynamicValueKeySub>> futures = new ArrayList<>();
        for (DynamicValueKey key : dynamicValues.keySet()) {
            switch (key.getSourceType()) {
                case CURRENT_TENANT:
                    futures.add(resolveEntityValue(tenantId, tenantId, key));
                    break;
                case CURRENT_CUSTOMER:
                    if (customerId != null && !customerId.isNullUid()) {
                        futures.add(resolveEntityValue(tenantId, customerId, key));
                    }
                    break;
                case CURRENT_USER:
                    if (userId != null && !userId.isNullUid()) {
                        futures.add(resolveEntityValue(tenantId, userId, key));
                    }
                    break;
            }
        }
        try {
            Map<EntityId, Map<String, DynamicValueKeySub>> tmpSubMap = new HashMap<>();
            for (DynamicValueKeySub sub : Futures.successfulAsList(futures).get()) {
                tmpSubMap.computeIfAbsent(sub.getEntityId(), tmp -> new HashMap<>()).put(sub.getKey().getSourceAttribute(), sub);
            }
            for (EntityId entityId : tmpSubMap.keySet()) {
                Map<String, Long> keyStates = new HashMap<>();
                Map<String, DynamicValueKeySub> dynamicValueKeySubMap = tmpSubMap.get(entityId);
                dynamicValueKeySubMap.forEach((k, v) -> keyStates.put(k, v.getLastUpdateTs()));
                int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
                TbAttributeSubscription sub = TbAttributeSubscription.builder()
                        .serviceId(serviceId)
                        .sessionId(sessionRef.getSessionId())
                        .subscriptionId(subIdx)
                        .tenantId(sessionRef.getSecurityCtx().getTenantId())
                        .entityId(entityId)
                        .updateProcessor((subscription, subscriptionUpdate) -> dynamicValueSubUpdate(subscription.getSessionId(), subscriptionUpdate, dynamicValueKeySubMap))
                        .queryTs(createdTime)
                        .allKeys(false)
                        .keyStates(keyStates)
                        .scope(TbAttributeSubscriptionScope.SERVER_SCOPE)
                        .build();
                subToDynamicValueKeySet.add(subIdx);
                localSubscriptionService.addSubscription(sub);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.info("[{}][{}][{}] Failed to resolve dynamic values: {}", tenantId, customerId, userId, dynamicValues.keySet());
        }

    }

    /**
     * 功能：执行 `dynamicValueSubUpdate` 对应的处理。
     * 参数：
     * - `sessionId`：会话ID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * - `dynamicValueKeySubMap`：键。
     * 返回：无。
     */
    private void dynamicValueSubUpdate(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate,
                                       Map<String, DynamicValueKeySub> dynamicValueKeySubMap) {
        Map<String, TsValue> latestUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            latestUpdate.put(k, new TsValue((Long) data[0], (String) data[1]));
        });

        boolean invalidateFilter = false;
        for (Map.Entry<String, TsValue> entry : latestUpdate.entrySet()) {
            String k = entry.getKey();
            TsValue tsValue = entry.getValue();
            DynamicValueKeySub sub = dynamicValueKeySubMap.get(k);
            if (sub.updateValue(tsValue)) {
                invalidateFilter = true;
                updateDynamicValuesByKey(sub, tsValue);
            }
        }

        if (invalidateFilter) {
            update();
        }
    }

    /**
     * 功能：判断`Dynamic`。
     * 参数：无。
     * 返回：判断结果。
     */
    public abstract boolean isDynamic();

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：无。
     */
    public abstract void fetchData();

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected abstract void update();

    /**
     * 功能：删除或清理`Subscriptions`。
     * 参数：无。
     * 返回：无。
     */
    public void clearSubscriptions() {
        clearDynamicValueSubscriptions();
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void stop() {
        stopped = true;
        cancelTasks();
        clearSubscriptions();
    }

    /**
     * 中文说明：
     * 1. 类目的：`DynamicValueKeySub` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    @Data
    private static class DynamicValueKeySub {
        /**
         * 键，用于定位映射、配置或数据项。
         */
        private final DynamicValueKey key;
        private final EntityId entityId;
        /**
         * 时间戳，用于标识当前数据或事件发生的时间。
         */
        private long lastUpdateTs;
        private String lastUpdateValue;

        /**
         * 功能：更新值。
         * 参数：
         * - `value`：值。
         * 返回：判断结果。
         */
        boolean updateValue(TsValue value) {
            if (value.getTs() > lastUpdateTs && (lastUpdateValue == null || !lastUpdateValue.equals(value.getValue()))) {
                this.lastUpdateTs = value.getTs();
                this.lastUpdateValue = value.getValue();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 功能：执行 `resolveEntityValue` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<DynamicValueKeySub> resolveEntityValue(TenantId tenantId, EntityId entityId, DynamicValueKey key) {
        ListenableFuture<Optional<AttributeKvEntry>> entry = attributesService.find(tenantId, entityId,
                TbAttributeSubscriptionScope.SERVER_SCOPE.name(), key.getSourceAttribute());
        return Futures.transform(entry, attributeOpt -> {
            DynamicValueKeySub sub = new DynamicValueKeySub(key, entityId);
            if (attributeOpt.isPresent()) {
                AttributeKvEntry attribute = attributeOpt.get();
                sub.setLastUpdateTs(attribute.getLastUpdateTs());
                sub.setLastUpdateValue(attribute.getValueAsString());
                updateDynamicValuesByKey(sub, new TsValue(attribute.getLastUpdateTs(), attribute.getValueAsString()));
            }
            return sub;
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：更新键。
     * 参数：
     * - `sub`：`sub` 参数。
     * - `tsValue`：值。
     * 返回：无。
     */
    @SuppressWarnings("unchecked")
    protected void updateDynamicValuesByKey(DynamicValueKeySub sub, TsValue tsValue) {
        DynamicValueKey dvk = sub.getKey();
        switch (dvk.getPredicateType()) {
            case STRING:
                dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(tsValue.getValue()));
                break;
            case NUMERIC:
                try {
                    Double dValue = Double.parseDouble(tsValue.getValue());
                    dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(dValue));
                } catch (NumberFormatException e) {
                    dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(null));
                }
                break;
            case BOOLEAN:
                Boolean bValue = Boolean.parseBoolean(tsValue.getValue());
                dynamicValues.get(dvk).forEach(dynamicValue -> dynamicValue.setResolvedValue(bValue));
                break;
        }
    }

    /**
     * 功能：保存或创建`Dynamic Values`。
     * 参数：
     * - `predicate`：`predicate` 参数。
     * 返回：无。
     */
    @SuppressWarnings("unchecked")
    private void registerDynamicValues(KeyFilterPredicate predicate) {
        switch (predicate.getType()) {
            case STRING:
            case NUMERIC:
            case BOOLEAN:
                Optional<DynamicValue> value = getDynamicValueFromSimplePredicate((SimpleKeyFilterPredicate) predicate);
                if (value.isPresent()) {
                    DynamicValue dynamicValue = value.get();
                    DynamicValueKey key = new DynamicValueKey(
                            predicate.getType(),
                            dynamicValue.getSourceType(),
                            dynamicValue.getSourceAttribute());
                    dynamicValues.computeIfAbsent(key, tmp -> new ArrayList<>()).add(dynamicValue);
                }
                break;
            case COMPLEX:
                ((ComplexFilterPredicate) predicate).getPredicates().forEach(this::registerDynamicValues);
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `predicate`：`predicate` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<DynamicValue<T>> getDynamicValueFromSimplePredicate(SimpleKeyFilterPredicate<T> predicate) {
        if (predicate.getValue().getUserValue() == null) {
            return Optional.ofNullable(predicate.getValue().getDynamicValue());
        } else {
            return Optional.empty();
        }
    }

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getSessionId() {
        return sessionRef.getSessionId();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantId getTenantId() {
        return sessionRef.getSecurityCtx().getTenantId();
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public CustomerId getCustomerId() {
        return sessionRef.getSecurityCtx().getCustomerId();
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    public UserId getUserId() {
        return sessionRef.getSecurityCtx().getId();
    }

    /**
     * 功能：删除或清理值。
     * 参数：无。
     * 返回：无。
     */
    protected void clearDynamicValueSubscriptions() {
        if (subToDynamicValueKeySet != null) {
            for (Integer subId : subToDynamicValueKeySet) {
                localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), subId);
            }
            subToDynamicValueKeySet.clear();
        }
    }

    /**
     * 功能：更新`Refresh Task`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：无。
     */
    public void setRefreshTask(ScheduledFuture<?> task) {
        if (!stopped) {
            this.refreshTask = task;
        } else {
            task.cancel(true);
        }
    }

    /**
     * 功能：执行 `cancelTasks` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void cancelTasks() {
        if (this.refreshTask != null) {
            log.trace("[{}][{}] Canceling old refresh task", sessionRef.getSessionId(), cmdId);
            this.refreshTask.cancel(true);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`DynamicValueKey` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    @Data
    public static class DynamicValueKey {
        /**
         * 类型，用于区分不同处理分支。
         */
        @Getter
        private final FilterPredicateType predicateType;
        /**
         * 类型，用于区分不同处理分支。
         */
        @Getter
        private final DynamicValueSourceType sourceType;
        /**
         * 属性，表示当前对象的对应属性。
         */
        @Getter
        private final String sourceAttribute;
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `update`：`update` 参数。
     * 返回：无。
     */
    public void sendWsMsg(CmdUpdate update) {
        wsLock.lock();
        try {
            wsService.sendUpdate(sessionRef.getSessionId(), update);
        } finally {
            wsLock.unlock();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbAbstractSubCtx` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

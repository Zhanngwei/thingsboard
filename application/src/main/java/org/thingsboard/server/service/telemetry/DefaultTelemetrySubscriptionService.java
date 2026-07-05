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
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.KvUtils;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`DefaultTelemetrySubscriptionService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
public class DefaultTelemetrySubscriptionService extends AbstractSubscriptionService implements TelemetrySubscriptionService {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final AttributesService attrService;
    private final TimeseriesService tsService;
    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    private final TbEntityViewService tbEntityViewService;
    private final TbApiUsageReportClient apiUsageClient;
    /**
     * 状态，提供当前类调用的业务操作。
     */
    private final TbApiUsageStateService apiUsageStateService;

    /**
     * 时间戳，负责处理对应任务或消息。
     */
    private ExecutorService tsCallBackExecutor;

    /**
     * 是否满足值条件。
     */
    @Value("${sql.ts.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    /**
     * 功能：创建 `DefaultTelemetrySubscriptionService` 实例，并初始化必要字段。
     * 参数：
     * - `attrService`：服务对象。
     * - `tsService`：服务对象。
     * - `tbEntityViewService`：服务对象。
     * - `apiUsageClient`：客户端对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public DefaultTelemetrySubscriptionService(AttributesService attrService,
                                               TimeseriesService tsService,
                                               @Lazy TbEntityViewService tbEntityViewService,
                                               TbApiUsageReportClient apiUsageClient,
                                               TbApiUsageStateService apiUsageStateService) {
        this.attrService = attrService;
        this.tsService = tsService;
        this.tbEntityViewService = tbEntityViewService;
        this.apiUsageClient = apiUsageClient;
        this.apiUsageStateService = apiUsageStateService;
    }

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initExecutor() {
        super.initExecutor();
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-service-ts-callback"));
    }

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getExecutorPrefix() {
        return "ts";
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
        super.shutdownExecutor();
    }

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAndNotify(TenantId tenantId, EntityId entityId, TsKvEntry ts) {
        SettableFuture<Void> future = SettableFuture.create();
        saveAndNotify(tenantId, entityId, Collections.singletonList(ts), new VoidFutureCallback(future));
        return future;
    }

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void saveAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, null, entityId, ts, 0L, callback);
    }

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAndNotify(TenantId tenantId, CustomerId customerId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback) {
        doSaveAndNotify(tenantId, customerId, entityId, ts, ttl, callback, true);
    }

    /**
     * 功能：保存或创建`Without Latest And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveWithoutLatestAndNotify(TenantId tenantId, CustomerId customerId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback) {
        doSaveAndNotify(tenantId, customerId, entityId, ts, ttl, callback, false);
    }

    /**
     * 功能：执行 `doSaveAndNotify` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void doSaveAndNotify(TenantId tenantId, CustomerId customerId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback, boolean saveLatest) {
        checkInternalEntity(entityId);
        boolean sysTenant = TenantId.SYS_TENANT_ID.equals(tenantId) || tenantId == null;
        if (sysTenant || apiUsageStateService.getApiUsageState(tenantId).isDbStorageEnabled()) {
            KvUtils.validate(ts, valueNoXssValidation);
            if (saveLatest) {
                saveAndNotifyInternal(tenantId, entityId, ts, ttl, getCallback(tenantId, customerId, sysTenant, callback));
            } else {
                saveWithoutLatestAndNotifyInternal(tenantId, entityId, ts, ttl, getCallback(tenantId, customerId, sysTenant, callback));
            }
        } else {
            callback.onFailure(new RuntimeException("DB storage writes are disabled due to API limits!"));
        }
    }

    /**
     * 功能：获取回调。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `sysTenant`：租户信息或租户标识。
     * - `callback`：处理完成后的回调。
     * 返回：异步处理结果。
     */
    private FutureCallback<Integer> getCallback(TenantId tenantId, CustomerId customerId, boolean sysTenant, FutureCallback<Void> callback) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Integer result) {
                if (!sysTenant && result != null && result > 0) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.STORAGE_DP_COUNT, result);
                }
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    /**
     * 功能：保存或创建`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Integer> callback) {
        saveAndNotifyInternal(tenantId, entityId, ts, 0L, callback);
    }

    /**
     * 功能：保存或创建`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `ttl`：`ttl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Integer> callback) {
        ListenableFuture<Integer> saveFuture = tsService.save(tenantId, entityId, ts, ttl);
        addMainCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onTimeSeriesUpdate(tenantId, entityId, ts));
        addEntityViewCallback(tenantId, entityId, ts);
    }

    /**
     * 功能：保存或创建`Without Latest And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `ttl`：`ttl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void saveWithoutLatestAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Integer> callback) {
        ListenableFuture<Integer> saveFuture = tsService.saveWithoutLatest(tenantId, entityId, ts, ttl);
        addMainCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onTimeSeriesUpdate(tenantId, entityId, ts));
    }

    /**
     * 功能：保存或创建实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * 返回：无。
     */
    private void addEntityViewCallback(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        if (EntityType.DEVICE.equals(entityId.getEntityType()) || EntityType.ASSET.equals(entityId.getEntityType())) {
            Futures.addCallback(this.tbEntityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable List<EntityView> result) {
                            if (result != null && !result.isEmpty()) {
                                Map<String, List<TsKvEntry>> tsMap = new HashMap<>();
                                for (TsKvEntry entry : ts) {
                                    tsMap.computeIfAbsent(entry.getKey(), s -> new ArrayList<>()).add(entry);
                                }
                                for (EntityView entityView : result) {
                                    List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                                            entityView.getKeys().getTimeseries() : new ArrayList<>(tsMap.keySet());
                                    List<TsKvEntry> entityViewLatest = new ArrayList<>();
                                    long startTs = entityView.getStartTimeMs();
                                    long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
                                    for (String key : keys) {
                                        List<TsKvEntry> entries = tsMap.get(key);
                                        if (entries != null) {
                                            Optional<TsKvEntry> tsKvEntry = entries.stream()
                                                    .filter(entry -> entry.getTs() > startTs && entry.getTs() <= endTs)
                                                    .max(Comparator.comparingLong(TsKvEntry::getTs));
                                            tsKvEntry.ifPresent(entityViewLatest::add);
                                        }
                                    }
                                    if (!entityViewLatest.isEmpty()) {
                                        saveLatestAndNotify(tenantId, entityView.getId(), entityViewLatest, new FutureCallback<>() {
                                            @Override
                                            public void onSuccess(@Nullable Void tmp) {
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {
                                            }
                                        });
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Error while finding entity views by tenantId and entityId", t);
                        }
                    }, MoreExecutors.directExecutor());
        }
    }

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, attributes, true, callback);
    }

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        saveAndNotifyInternal(tenantId, entityId, scope, attributes, notifyDevice, callback);
    }

    /**
     * 功能：保存或创建`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, FutureCallback<Void> callback) {
        ListenableFuture<List<String>> saveFuture = attrService.save(tenantId, entityId, scope, attributes);
        addVoidCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onAttributesUpdate(tenantId, entityId, scope, attributes, notifyDevice));
    }

    /**
     * 功能：保存或创建`Latest And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void saveLatestAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        saveLatestAndNotifyInternal(tenantId, entityId, ts, callback);
    }

    /**
     * 功能：保存或创建`Latest And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void saveLatestAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback) {
        ListenableFuture<List<Void>> saveFuture = tsService.saveLatest(tenantId, entityId, ts);
        addVoidCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onTimeSeriesUpdate(tenantId, entityId, ts));
    }

    /**
     * 功能：删除或清理`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        deleteAndNotifyInternal(tenantId, entityId, scope, keys, false, callback);
    }

    /**
     * 功能：删除或清理`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        deleteAndNotifyInternal(tenantId, entityId, scope, keys, notifyDevice, callback);
    }

    /**
     * 功能：删除或清理`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void deleteAndNotifyInternal(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, FutureCallback<Void> callback) {
        ListenableFuture<List<String>> deleteFuture = attrService.removeAll(tenantId, entityId, scope, keys);
        addVoidCallback(deleteFuture, callback);
        addWsCallback(deleteFuture, success -> onAttributesDelete(tenantId, entityId, scope, keys, notifyDevice));
    }

    /**
     * 功能：删除或清理`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void deleteLatest(TenantId tenantId, EntityId entityId, List<String> keys, FutureCallback<Void> callback) {
        checkInternalEntity(entityId);
        deleteLatestInternal(tenantId, entityId, keys, callback);
    }

    /**
     * 功能：删除或清理`Latest Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void deleteLatestInternal(TenantId tenantId, EntityId entityId, List<String> keys, FutureCallback<Void> callback) {
        ListenableFuture<List<TsKvLatestRemovingResult>> deleteFuture = tsService.removeLatest(tenantId, entityId, keys);
        addVoidCallback(deleteFuture, callback);
    }

    /**
     * 功能：删除或清理`All Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void deleteAllLatest(TenantId tenantId, EntityId entityId, FutureCallback<Collection<String>> callback) {
        ListenableFuture<Collection<String>> deleteFuture = tsService.removeAllLatest(tenantId, entityId);
        Futures.addCallback(deleteFuture, new FutureCallback<Collection<String>>() {
            @Override
            public void onSuccess(@Nullable Collection<String> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    /**
     * 功能：删除或清理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `deleteTsKvQueries`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void deleteTimeseriesAndNotify(TenantId tenantId, EntityId entityId, List<String> keys, List<DeleteTsKvQuery> deleteTsKvQueries, FutureCallback<Void> callback) {
        ListenableFuture<List<TsKvLatestRemovingResult>> deleteFuture = tsService.remove(tenantId, entityId, deleteTsKvQueries);
        addVoidCallback(deleteFuture, callback);
        addWsCallback(deleteFuture, list -> onTimeSeriesDelete(tenantId, entityId, keys, list));
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new DoubleDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value) {
        SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value) {
        SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value) {
        SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value) {
        SettableFuture<Void> future = SettableFuture.create();
        saveAttrAndNotify(tenantId, entityId, scope, key, value, new VoidFutureCallback(future));
        return future;
    }

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            subscriptionManagerService.onAttributesUpdate(tenantId, entityId, scope, attributes, notifyDevice, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toAttributesUpdateProto(tenantId, entityId, scope, attributes);
        });
    }

    /**
     * 功能：处理`on Attributes Delete`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            subscriptionManagerService.onAttributesDelete(tenantId, entityId, scope, keys, notifyDevice, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toAttributesDeleteProto(tenantId, entityId, scope, keys, notifyDevice);
        });
    }

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * 返回：无。
     */
    private void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            subscriptionManagerService.onTimeSeriesUpdate(tenantId, entityId, ts, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toTimeseriesUpdateProto(tenantId, entityId, ts);
        });
    }

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `ts`：时间戳。
     * 返回：无。
     */
    private void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, List<TsKvLatestRemovingResult> ts) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            List<TsKvEntry> updated = new ArrayList<>();
            List<String> deleted = new ArrayList<>();

            ts.stream().filter(Objects::nonNull).forEach(res -> {
                if (res.isRemoved()) {
                    if (res.getData() != null) {
                        updated.add(res.getData());
                    } else {
                        deleted.add(res.getKey());
                    }
                }
            });

            subscriptionManagerService.onTimeSeriesUpdate(tenantId, entityId, updated, TbCallback.EMPTY);
            subscriptionManagerService.onTimeSeriesDelete(tenantId, entityId, deleted, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toTimeseriesDeleteProto(tenantId, entityId, keys);
        });
    }

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `saveFuture`：数据列表。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private <S> void addVoidCallback(ListenableFuture<S> saveFuture, final FutureCallback<Void> callback) {
        if (callback == null) return;
        Futures.addCallback(saveFuture, new FutureCallback<S>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `saveFuture`：数据列表。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private <S> void addMainCallback(ListenableFuture<S> saveFuture, final FutureCallback<S> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<S>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    private void checkInternalEntity(EntityId entityId) {
        if (EntityType.API_USAGE_STATE.equals(entityId.getEntityType())) {
            throw new RuntimeException("Can't update API Usage State!");
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`VoidFutureCallback` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private static class VoidFutureCallback implements FutureCallback<Void> {
        /**
         * 异步结果集合，用于去重保存或快速判断对象是否存在。
         */
        private final SettableFuture<Void> future;

        /**
         * 功能：创建 `DefaultTelemetrySubscriptionService` 实例，并初始化必要字段。
         * 参数：
         * - `future`：`future` 参数。
         * 返回：新创建的对象实例。
         */
        public VoidFutureCallback(SettableFuture<Void> future) {
            this.future = future;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `result`：`result` 参数。
         * 返回：无。
         */
        @Override
        public void onSuccess(Void result) {
            future.set(null);
        }

        /**
         * 功能：处理失败信息。
         * 参数：
         * - `t`：`t` 参数。
         * 返回：无。
         */
        @Override
        public void onFailure(Throwable t) {
            future.setException(t);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTelemetrySubscriptionService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */

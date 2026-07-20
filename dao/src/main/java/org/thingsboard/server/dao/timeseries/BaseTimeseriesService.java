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
package org.thingsboard.server.dao.timeseries;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isBlank;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `BaseTimeseriesService` 是 ThingsBoard DAO 中负责时序数据的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TimeseriesService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@SuppressWarnings("UnstableApiUsage")
@Service
@Slf4j
public class BaseTimeseriesService implements TimeseriesService {

    /**
     * `INSERTS_PER_ENTRY`常量，用于统一引用固定值。
     */
    private static final int INSERTS_PER_ENTRY = 3;
    private static final int INSERTS_PER_ENTRY_WITHOUT_LATEST = 2;
    /**
     * `DELETES_PER_ENTRY`常量，用于统一引用固定值。
     */
    private static final int DELETES_PER_ENTRY = INSERTS_PER_ENTRY;
    public static final Function<List<Integer>, Integer> SUM_ALL_INTEGERS = new Function<>() {
        @Override
        public @Nullable Integer apply(@Nullable List<Integer> input) {
            int result = 0;
            if (input != null) {
                for (Integer tmp : input) {
                    if (tmp != null) {
                        result += tmp;
                    }
                }
            }
            return result;
        }
    };

    /**
     * 时间戳，用于控制时间范围或等待时长。
     */
    @Value("${database.ts_max_intervals}")
    private long maxTsIntervals;

    /**
     * 时序数据，用于读取或保存对应领域对象。
     */
    @Autowired
    private TimeseriesDao timeseriesDao;

    /**
     * 时序数据，用于读取或保存对应领域对象。
     */
    @Autowired
    private TimeseriesLatestDao timeseriesLatestDao;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityViewService entityViewService;

    /**
     * 功能：获取`All By Queries`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllByQueries(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        validate(entityId);
        queries.forEach(this::validate);
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, (EntityViewId) entityId);
            List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                    entityView.getKeys().getTimeseries() : Collections.emptyList();
            List<ReadTsKvQuery> filteredQueries =
                    queries.stream()
                            .filter(query -> keys.isEmpty() || keys.contains(query.getKey()))
                            .collect(Collectors.toList());
            return timeseriesDao.findAllAsync(tenantId, entityView.getEntityId(), updateQueriesForEntityView(entityView, filteredQueries));
        }
        return timeseriesDao.findAllAsync(tenantId, entityId, queries);
    }

    /**
     * 功能：获取`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<TsKvEntry>> findAll(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return Futures.transform(findAllByQueries(tenantId, entityId, queries),
                result -> {
                    if (result != null && !result.isEmpty()) {
                        return result.stream().map(ReadTsKvQueryResult::getData).flatMap(Collection::stream).collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, String key) {
        validate(entityId);
        return timeseriesLatestDao.findLatestOpt(tenantId, entityId, key);
    }

    /**
     * 功能：获取`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ListenableFuture<TsKvEntry>> futures = new ArrayList<>(keys.size());
        keys.forEach(key -> Validator.validateString(key, k -> "Incorrect key " + k));
        for (String key : keys) {
            futures.add(timeseriesLatestDao.findLatest(tenantId, entityId, key));
        }
        return Futures.allAsList(futures);
    }

    /**
     * 功能：获取`Latest Sync`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TsKvEntry> findLatestSync(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<TsKvEntry> latestEntries = new ArrayList<>(keys.size());
        keys.forEach(key -> Validator.validateString(key, k -> "Incorrect key " + k));
        for (String key : keys) {
            latestEntries.add(timeseriesLatestDao.findLatestSync(tenantId, entityId, key));
        }
        return latestEntries;
    }

    /**
     * 功能：获取`All Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        validate(entityId);
        return timeseriesLatestDao.findAllLatest(tenantId, entityId);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return timeseriesLatestDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return timeseriesLatestDao.findAllKeysByEntityIds(tenantId, entityIds);
    }

    /**
     * 功能：执行 `cleanup` 对应的处理。
     * 参数：
     * - `systemTtl`：`systemTtl` 参数。
     * 返回：无。
     */
    @Override
    public void cleanup(long systemTtl) {
        timeseriesDao.cleanup(systemTtl);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        validate(entityId);
        List<ListenableFuture<Integer>> futures = new ArrayList<>(INSERTS_PER_ENTRY);
        saveAndRegisterFutures(tenantId, futures, entityId, tsKvEntry, 0L);
        return Futures.transform(Futures.allAsList(futures), SUM_ALL_INTEGERS, MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntries`：数据列表。
     * - `ttl`：`ttl` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        return doSave(tenantId, entityId, tsKvEntries, ttl, true);
    }

    /**
     * 功能：保存或创建`Without Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntries`：数据列表。
     * - `ttl`：`ttl` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Integer> saveWithoutLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        return doSave(tenantId, entityId, tsKvEntries, ttl, false);
    }

    /**
     * 功能：执行 `doSave` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntries`：数据列表。
     * - `ttl`：`ttl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Integer> doSave(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl, boolean saveLatest) {
        int inserts = saveLatest ? INSERTS_PER_ENTRY : INSERTS_PER_ENTRY_WITHOUT_LATEST;
        List<ListenableFuture<Integer>> futures = new ArrayList<>(tsKvEntries.size() * inserts);
        for (TsKvEntry tsKvEntry : tsKvEntries) {
            if (saveLatest) {
                saveAndRegisterFutures(tenantId, futures, entityId, tsKvEntry, ttl);
            } else {
                saveWithoutLatestAndRegisterFutures(tenantId, futures, entityId, tsKvEntry, ttl);
            }
        }
        return Futures.transform(Futures.allAsList(futures), SUM_ALL_INTEGERS, MoreExecutors.directExecutor());
    }

    /**
     * 功能：保存或创建`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntries`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Void>> saveLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries) {
        List<ListenableFuture<Void>> futures = new ArrayList<>(tsKvEntries.size());
        for (TsKvEntry tsKvEntry : tsKvEntries) {
            futures.add(timeseriesLatestDao.saveLatest(tenantId, entityId, tsKvEntry));
        }
        return Futures.allAsList(futures);
    }

    /**
     * 功能：保存或创建`And Register Futures`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `futures`：数据列表。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void saveAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        doSaveAndRegisterFuturesFor(tenantId, futures, entityId, tsKvEntry, ttl);
        futures.add(Futures.transform(timeseriesLatestDao.saveLatest(tenantId, entityId, tsKvEntry), v -> 0, MoreExecutors.directExecutor()));
    }

    /**
     * 功能：保存或创建`Without Latest And Register Futures`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `futures`：数据列表。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void saveWithoutLatestAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        doSaveAndRegisterFuturesFor(tenantId, futures, entityId, tsKvEntry, ttl);
    }

    /**
     * 功能：执行 `doSaveAndRegisterFuturesFor` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `futures`：数据列表。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void doSaveAndRegisterFuturesFor(TenantId tenantId, List<ListenableFuture<Integer>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            throw new IncorrectParameterException("Telemetry data can't be stored for entity view. Read only");
        }
        futures.add(timeseriesDao.savePartition(tenantId, entityId, tsKvEntry.getTs(), tsKvEntry.getKey()));
        futures.add(timeseriesDao.save(tenantId, entityId, tsKvEntry, ttl));
    }

    /**
     * 功能：更新实体视图。
     * 参数：
     * - `entityView`：实体对象。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<ReadTsKvQuery> updateQueriesForEntityView(EntityView entityView, List<ReadTsKvQuery> queries) {
        return queries.stream().map(query -> {
            long startTs;
            if (entityView.getStartTimeMs() != 0 && entityView.getStartTimeMs() > query.getStartTs()) {
                startTs = entityView.getStartTimeMs();
            } else {
                startTs = query.getStartTs();
            }

            long endTs;
            if (entityView.getEndTimeMs() != 0 && entityView.getEndTimeMs() < query.getEndTs()) {
                endTs = entityView.getEndTimeMs();
            } else {
                endTs = query.getEndTs();
            }
            return new BaseReadTsKvQuery(query, startTs, endTs);
        }).collect(Collectors.toList());
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `deleteTsKvQueries`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<TsKvLatestRemovingResult>> remove(TenantId tenantId, EntityId entityId, List<DeleteTsKvQuery> deleteTsKvQueries) {
        validate(entityId);
        deleteTsKvQueries.forEach(BaseTimeseriesService::validate);
        List<ListenableFuture<TsKvLatestRemovingResult>> futures = new ArrayList<>(deleteTsKvQueries.size() * DELETES_PER_ENTRY);
        for (DeleteTsKvQuery tsKvQuery : deleteTsKvQueries) {
            deleteAndRegisterFutures(tenantId, futures, entityId, tsKvQuery);
        }
        return Futures.allAsList(futures);
    }

    /**
     * 功能：删除或清理`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<TsKvLatestRemovingResult>> removeLatest(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ListenableFuture<TsKvLatestRemovingResult>> futures = new ArrayList<>(keys.size());
        for (String key : keys) {
            DeleteTsKvQuery query = new BaseDeleteTsKvQuery(key, 0, System.currentTimeMillis(), false);
            futures.add(timeseriesLatestDao.removeLatest(tenantId, entityId, query));
        }
        return Futures.allAsList(futures);
    }

    /**
     * 功能：删除或清理`All Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Collection<String>> removeAllLatest(TenantId tenantId, EntityId entityId) {
        validate(entityId);
        return Futures.transformAsync(this.findAllLatest(tenantId, entityId), latest -> {
            if (latest != null && !latest.isEmpty()) {
                Collection<String> keys = latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList());
                return Futures.transform(this.removeLatest(tenantId, entityId, keys), res -> keys, MoreExecutors.directExecutor());
            } else {
                return Futures.immediateFuture(Collections.emptyList());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：删除或清理`And Register Futures`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `futures`：数据列表。
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：无。
     */
    private void deleteAndRegisterFutures(TenantId tenantId, List<ListenableFuture<TsKvLatestRemovingResult>> futures, EntityId entityId, DeleteTsKvQuery query) {
        futures.add(Futures.transform(timeseriesDao.remove(tenantId, entityId, query), v -> null, MoreExecutors.directExecutor()));
        if (query.getDeleteLatest()) {
            futures.add(timeseriesLatestDao.removeLatest(tenantId, entityId, query));
        }
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    private static void validate(EntityId entityId) {
        Validator.validateEntityId(entityId, id -> "Incorrect entityId " + id);
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：无。
     */
    private void validate(ReadTsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("ReadTsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect ReadTsKvQuery. Key can't be empty");
        } else if (query.getAggregation() == null) {
            throw new IncorrectParameterException("Incorrect ReadTsKvQuery. Aggregation can't be empty");
        }
        if (!Aggregation.NONE.equals(query.getAggregation())) {
            long step = Math.max(query.getInterval(), 1000);
            long intervalCounts = (query.getEndTs() - query.getStartTs()) / step;
            if (intervalCounts > maxTsIntervals || intervalCounts < 0) {
                throw new IncorrectParameterException("Incorrect TsKvQuery. Number of intervals is to high - " + intervalCounts + ". " +
                        "Please increase 'interval' parameter for your query or reduce the time range of the query.");
            }
        }
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：无。
     */
    private static void validate(DeleteTsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("DeleteTsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect DeleteTsKvQuery. Key can't be empty");
        }
    }
}

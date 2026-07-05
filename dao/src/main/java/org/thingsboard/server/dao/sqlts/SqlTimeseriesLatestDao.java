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
package org.thingsboard.server.dao.sqlts;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.sqlts.latest.SearchTsKvLatestRepository;
import org.thingsboard.server.dao.sqlts.latest.TsKvLatestRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`SqlTimeseriesLatestDao` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@Slf4j
@Component
@SqlTsLatestAnyDao
public class SqlTimeseriesLatestDao extends BaseAbstractSqlTimeseriesDao implements TimeseriesLatestDao {

    /**
     * `DESC_ORDER`常量，用于统一引用固定值。
     */
    private static final String DESC_ORDER = "DESC";

    /**
     * 时间戳，用于读取或保存对应领域对象。
     */
    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

    /**
     * 时序数据，用于读取或保存对应领域对象。
     */
    @Autowired
    protected AggregationTimeseriesDao aggregationTimeseriesDao;

    /**
     * 时间戳，用于读取或保存对应领域对象。
     */
    @Autowired
    private SearchTsKvLatestRepository searchTsKvLatestRepository;

    /**
     * 时间戳，用于读取或保存对应领域对象。
     */
    @Autowired
    private InsertLatestTsRepository insertLatestTsRepository;

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private TbSqlBlockingQueueWrapper<TsKvLatestEntity> tsLatestQueue;

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    @Value("${sql.ts_latest.batch_size:1000}")
    private int tsLatestBatchSize;

    /**
     * 最大延迟，用于控制时间范围或等待时长。
     */
    @Value("${sql.ts_latest.batch_max_delay:100}")
    private long tsLatestMaxDelay;

    /**
     * 统计日志输出间隔，用于控制时间范围或等待时长。
     */
    @Value("${sql.ts_latest.stats_print_interval_ms:1000}")
    private long tsLatestStatsPrintIntervalMs;

    /**
     * 批处理线程数，用于控制处理规模或位置。
     */
    @Value("${sql.ts_latest.batch_threads:4}")
    private int tsLatestBatchThreads;

    /**
     * 是否启用`batch sort`。
     */
    @Value("${sql.batch_sort:true}")
    protected boolean batchSortEnabled;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Autowired
    private StatsFactory statsFactory;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    protected void init() {
        TbSqlBlockingQueueParams tsLatestParams = TbSqlBlockingQueueParams.builder()
                .logName("TS Latest")
                .batchSize(tsLatestBatchSize)
                .maxDelay(tsLatestMaxDelay)
                .statsPrintIntervalMs(tsLatestStatsPrintIntervalMs)
                .statsNamePrefix("ts.latest")
                .batchSortEnabled(false)
                .build();

        java.util.function.Function<TsKvLatestEntity, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
        tsLatestQueue = new TbSqlBlockingQueueWrapper<>(tsLatestParams, hashcodeFunction, tsLatestBatchThreads, statsFactory);

        tsLatestQueue.init(logExecutor, v -> {
            Map<TsKey, TsKvLatestEntity> trueLatest = new HashMap<>();
            v.forEach(ts -> {
                TsKey key = new TsKey(ts.getEntityId(), ts.getKey());
                trueLatest.merge(key, ts, (oldTs, newTs) -> oldTs.getTs() <= newTs.getTs() ? newTs : oldTs);
            });
            List<TsKvLatestEntity> latestEntities = new ArrayList<>(trueLatest.values());
            if (batchSortEnabled) {
                latestEntities.sort(Comparator.comparing((Function<TsKvLatestEntity, UUID>) AbstractTsKvEntity::getEntityId)
                        .thenComparingInt(AbstractTsKvEntity::getKey));
            }
            insertLatestTsRepository.saveOrUpdate(latestEntities);
        }, (l, r) -> 0);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    protected void destroy() {
        if (tsLatestQueue != null) {
            tsLatestQueue.destroy();
        }
    }

    /**
     * 功能：保存或创建`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    /**
     * 功能：删除或清理`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    /**
     * 功能：获取`Latest Opt`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, EntityId entityId, String key) {
        return service.submit(() -> Optional.ofNullable(doFindLatest(entityId, key)));
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
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return service.submit(() -> getLatestTsKvEntry(entityId, key));
    }

    /**
     * 功能：获取`Latest Sync`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public TsKvEntry findLatestSync(TenantId tenantId, EntityId entityId, String key) {
        return getLatestTsKvEntry(entityId, key);
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
        return getFindAllLatestFuture(entityId);
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
        if (deviceProfileId != null) {
            return tsKvLatestRepository.getKeysByDeviceProfileId(tenantId.getId(), deviceProfileId.getId());
        } else {
            return tsKvLatestRepository.getKeysByTenantId(tenantId.getId());
        }
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
        return tsKvLatestRepository.findAllKeysByEntityIds(entityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TsKvLatestRemovingResult> getNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<List<TsKvEntry>> future = findNewLatestEntryFuture(tenantId, entityId, query);
        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                TsKvEntry entry = entryList.get(0);
                return Futures.transform(getSaveLatestFuture(entityId, entry), v -> new TsKvLatestRemovingResult(entry), MoreExecutors.directExecutor());
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), true));
        }, service);
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<TsKvEntry>> findNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        return Futures.transform(aggregationTimeseriesDao.findAllAsync(tenantId, entityId, findNewLatestQuery),
                ReadTsKvQueryResult::getData, MoreExecutors.directExecutor());
    }

   /**
    * 功能：执行 `doFindLatest` 对应的处理。
    * 参数：
    * - `entityId`：实体IDID。
    * - `key`：键。
    * 返回：处理结果。
    */
   protected TsKvEntry doFindLatest(EntityId entityId, String key) {
        TsKvLatestCompositeKey compositeKey =
                new TsKvLatestCompositeKey(
                        entityId.getId(),
                        getOrSaveKeyId(key));
        Optional<TsKvLatestEntity> entry = tsKvLatestRepository.findById(compositeKey);
        if (entry.isPresent()) {
            TsKvLatestEntity tsKvLatestEntity = entry.get();
            tsKvLatestEntity.setStrKey(key);
            return DaoUtil.getData(tsKvLatestEntity);
        } else {
            return null;
        }
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<TsKvLatestRemovingResult> getRemoveLatestFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<TsKvEntry> latestFuture = service.submit(() -> doFindLatest(entityId, query.getKey()));
        return Futures.transformAsync(latestFuture, latest -> {
            if (latest == null) {
                return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), false));
            }
            boolean isRemoved = false;
            long ts = latest.getTs();
            if (ts >= query.getStartTs() && ts < query.getEndTs()) {
                TsKvLatestEntity latestEntity = new TsKvLatestEntity();
                latestEntity.setEntityId(entityId.getId());
                latestEntity.setKey(getOrSaveKeyId(query.getKey()));
                tsKvLatestRepository.delete(latestEntity);
                isRemoved = true;
                if (query.getRewriteLatestIfDeleted()) {
                    return getNewLatestEntryFuture(tenantId, entityId, query);
                }
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), isRemoved));
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<List<TsKvEntry>> getFindAllLatestFuture(EntityId entityId) {
        return service.submit(() ->
                DaoUtil.convertDataList(Lists.newArrayList(
                        searchTsKvLatestRepository.findAllByEntityId(entityId.getId()))));
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<Void> getSaveLatestFuture(EntityId entityId, TsKvEntry tsKvEntry) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityId(entityId.getId());
        latestEntity.setTs(tsKvEntry.getTs());
        latestEntity.setKey(getOrSaveKeyId(tsKvEntry.getKey()));
        latestEntity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        latestEntity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        latestEntity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        latestEntity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        latestEntity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));

        return tsLatestQueue.add(latestEntity);
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    private TsKvEntry getLatestTsKvEntry(EntityId entityId, String key) {
        TsKvEntry latest = doFindLatest(entityId, key);
        if (latest == null) {
            latest = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
        return latest;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SqlTimeseriesLatestDao` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */

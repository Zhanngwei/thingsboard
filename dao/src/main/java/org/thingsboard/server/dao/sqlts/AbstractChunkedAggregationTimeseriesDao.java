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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.insert.InsertTsRepository;
import org.thingsboard.server.dao.sqlts.ts.TsKvRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.TimeUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * 中文说明：
 * 1. 类目的：`AbstractChunkedAggregationTimeseriesDao` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@SuppressWarnings("UnstableApiUsage")
@Slf4j
public abstract class AbstractChunkedAggregationTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    /**
     * 时间戳，用于读取或保存对应领域对象。
     */
    @Autowired
    protected TsKvRepository tsKvRepository;

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    protected InsertTsRepository<TsKvEntity> insertRepository;

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    protected TbSqlBlockingQueueWrapper<TsKvEntity> tsQueue;
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
        TbSqlBlockingQueueParams tsParams = TbSqlBlockingQueueParams.builder()
                .logName("TS")
                .batchSize(tsBatchSize)
                .maxDelay(tsMaxDelay)
                .statsPrintIntervalMs(tsStatsPrintIntervalMs)
                .statsNamePrefix("ts")
                .batchSortEnabled(batchSortEnabled)
                .build();

        Function<TsKvEntity, Integer> hashcodeFunction = entity -> entity.getEntityId().hashCode();
        tsQueue = new TbSqlBlockingQueueWrapper<>(tsParams, hashcodeFunction, tsBatchThreads, statsFactory);
        tsQueue.init(logExecutor, v -> insertRepository.saveOrUpdate(v),
                Comparator.comparing((Function<TsKvEntity, UUID>) AbstractTsKvEntity::getEntityId)
                        .thenComparing(AbstractTsKvEntity::getKey)
                        .thenComparing(AbstractTsKvEntity::getTs)
        );
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    protected void destroy() {
        if (tsQueue != null) {
            tsQueue.destroy();
        }
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> {
            tsKvRepository.delete(
                    entityId.getId(),
                    getOrSaveKeyId(query.getKey()),
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    /**
     * 功能：保存或创建分区。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntryTs`：时间戳。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key) {
        return Futures.immediateFuture(null);
    }

    /**
     * 功能：获取`All Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    /**
     * 功能：获取`All Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        var aggParams = query.getAggParameters();
        if (Aggregation.NONE.equals(aggParams.getAggregation())) {
            return Futures.immediateFuture(findAllAsyncWithLimit(entityId, query));
        } else {
            List<ListenableFuture<Optional<TsKvEntity>>> futures = new ArrayList<>();
            var intervalType = aggParams.getIntervalType();
            long startPeriod = query.getStartTs();
            long endPeriod = Math.max(query.getStartTs() + 1, query.getEndTs());
            while (startPeriod < endPeriod) {
                long startTs = startPeriod;
                long endTs;
                if (IntervalType.MILLISECONDS.equals(intervalType)) {
                    endTs = startPeriod + aggParams.getInterval();
                } else {
                    endTs = TimeUtils.calculateIntervalEnd(startTs, intervalType, aggParams.getTzId());
                }
                endTs = Math.min(endTs, endPeriod);
                long ts = startTs + (endTs - startTs) / 2;
                ListenableFuture<Optional<TsKvEntity>> aggregateTsKvEntry = findAndAggregateAsync(entityId, query.getKey(), startTs, endTs, ts, query.getAggregation());
                futures.add(aggregateTsKvEntry);
                startPeriod = endTs;
            }
            return getReadTsKvQueryResultFuture(query, Futures.allAsList(futures));
        }
    }

    /**
     * 功能：获取数量限制。
     * 参数：
     * - `entityId`：实体IDID。
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    private ReadTsKvQueryResult findAllAsyncWithLimit(EntityId entityId, ReadTsKvQuery query) {
        Integer keyId = getOrSaveKeyId(query.getKey());
        List<TsKvEntity> tsKvEntities = tsKvRepository.findAllWithLimit(
                entityId.getId(),
                keyId,
                query.getStartTs(),
                query.getEndTs(),
                PageRequest.ofSize(query.getLimit()).withSort(Direction.fromString(query.getOrder()), "ts"));
        tsKvEntities.forEach(tsKvEntity -> tsKvEntity.setStrKey(query.getKey()));
        List<TsKvEntry> tsKvEntries = DaoUtil.convertDataList(tsKvEntities);
        long lastTs = tsKvEntries.stream().map(TsKvEntry::getTs).max(Long::compare).orElse(query.getStartTs());
        return new ReadTsKvQueryResult(query.getId(), tsKvEntries, lastTs);
    }

    /**
     * 功能：获取`And Aggregate Async`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Optional<TsKvEntity>> findAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation) {
        return service.submit(() -> {
            TsKvEntity entity = switchAggregation(entityId, key, startTs, endTs, aggregation);
            if (entity != null && entity.isNotEmpty()) {
                entity.setEntityId(entityId.getId());
                entity.setStrKey(key);
                entity.setTs(ts);
                return Optional.of(entity);
            } else {
                return Optional.empty();
            }
        });
    }

    /**
     * 功能：执行 `switchAggregation` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected TsKvEntity switchAggregation(EntityId entityId, String key, long startTs, long endTs, Aggregation aggregation) {
        var keyId = getOrSaveKeyId(key);
        switch (aggregation) {
            case AVG:
                return tsKvRepository.findAvg(entityId.getId(), keyId, startTs, endTs);
            case MAX:
                var max = tsKvRepository.findNumericMax(entityId.getId(), keyId, startTs, endTs);
                if (max.isNotEmpty()) {
                    return max;
                } else {
                    return tsKvRepository.findStringMax(entityId.getId(), keyId, startTs, endTs);
                }
            case MIN:
                var min = tsKvRepository.findNumericMin(entityId.getId(), keyId, startTs, endTs);
                if (min.isNotEmpty()) {
                    return min;
                } else {
                    return tsKvRepository.findStringMin(entityId.getId(), keyId, startTs, endTs);
                }
            case SUM:
                return tsKvRepository.findSum(entityId.getId(), keyId, startTs, endTs);
            case COUNT:
                return tsKvRepository.findCount(entityId.getId(), keyId, startTs, endTs);
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractChunkedAggregationTimeseriesDao` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */

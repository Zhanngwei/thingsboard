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

import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.nosql.TbResultSet;
import org.thingsboard.server.dao.sqlts.AggregationTimeseriesDao;
import org.thingsboard.server.dao.util.NoSqlTsLatestDao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

/**
 * 中文说明：
 * 1. 类目的：`CassandraBaseTimeseriesLatestDao` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@Component
@Slf4j
@NoSqlTsLatestDao
public class CassandraBaseTimeseriesLatestDao extends AbstractCassandraBaseTimeseriesDao implements TimeseriesLatestDao {

    /**
     * 时序数据，用于读取或保存对应领域对象。
     */
    @Autowired
    protected AggregationTimeseriesDao aggregationTimeseriesDao;

    /**
     * `latestInsertStmt` 字段，保存当前对象的对应属性。
     */
    private PreparedStatement latestInsertStmt;
    private PreparedStatement findLatestStmt;
    /**
     * `findAllLatestStmt` 字段，保存当前对象的对应属性。
     */
    private PreparedStatement findAllLatestStmt;

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
        return findLatest(tenantId, entityId, key, rs -> convertResultToTsKvEntryOpt(key, rs.one()));
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
        return findLatest(tenantId, entityId, key, rs -> convertResultToTsKvEntry(key, rs.one()));
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
        try {
            return findLatest(tenantId, entityId, key, rs -> convertResultToTsKvEntry(key, rs.one())).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("[{}][{}] Failed to get latest entry for key: {} due to: ", tenantId, entityId, key, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：获取`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `function`：`function` 参数。
     * 返回：匹配的数据集合。
     */
    private <T> ListenableFuture<T> findLatest(TenantId tenantId, EntityId entityId, String key, java.util.function.Function<TbResultSet, T> function) {
        BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(getFindLatestStmt().bind());
        stmtBuilder.setString(0, entityId.getEntityType().name());
        stmtBuilder.setUuid(1, entityId.getId());
        stmtBuilder.setString(2, key);
        BoundStatement stmt = stmtBuilder.build();
        log.debug(GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID, stmt, entityId.getEntityType(), entityId.getId());
        return getFuture(executeAsyncRead(tenantId, stmt), function);
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
        BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(getFindAllLatestStmt().bind());
        stmtBuilder.setString(0, entityId.getEntityType().name());
        stmtBuilder.setUuid(1, entityId.getId());
        BoundStatement stmt = stmtBuilder.build();
        log.debug(GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID, stmt, entityId.getEntityType(), entityId.getId());
        return getFutureAsync(executeAsyncRead(tenantId, stmt), rs -> convertAsyncResultSetToTsKvEntryList(rs));
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
        return Collections.emptyList();
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
        return Collections.emptyList();
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
        BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(getLatestStmt().bind());
        stmtBuilder.setString(0, entityId.getEntityType().name())
                .setUuid(1, entityId.getId())
                .setString(2, tsKvEntry.getKey())
                .setLong(3, tsKvEntry.getTs())
                .set(4, tsKvEntry.getBooleanValue().orElse(null), Boolean.class)
                .set(5, tsKvEntry.getStrValue().orElse(null), String.class)
                .set(6, tsKvEntry.getLongValue().orElse(null), Long.class)
                .set(7, tsKvEntry.getDoubleValue().orElse(null), Double.class);
        Optional<String> jsonV = tsKvEntry.getJsonValue();
        if (jsonV.isPresent()) {
            stmtBuilder.setString(8, tsKvEntry.getJsonValue().get());
        } else {
            stmtBuilder.setToNull(8);
        }
        BoundStatement stmt = stmtBuilder.build();

        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> null);
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
        ListenableFuture<TsKvEntry> latestEntryFuture = findLatest(tenantId, entityId, query.getKey());

        ListenableFuture<Boolean> booleanFuture = Futures.transform(latestEntryFuture, latestEntry -> {
            long ts = latestEntry.getTs();
            if (ts > query.getStartTs() && ts <= query.getEndTs()) {
                return true;
            } else {
                log.trace("Won't be deleted latest value for [{}], key - {}", entityId, query.getKey());
            }
            return false;
        }, readResultsProcessingExecutor);

        ListenableFuture<Boolean> removedLatestFuture = Futures.transformAsync(booleanFuture, isRemove -> {
            if (isRemove) {
                return Futures.transform(deleteLatest(tenantId, entityId, query.getKey()), res -> true, MoreExecutors.directExecutor());
            }
            return Futures.immediateFuture(false);
        }, readResultsProcessingExecutor);

        return Futures.transformAsync(removedLatestFuture, isRemoved -> {
            if (isRemoved && query.getRewriteLatestIfDeleted()) {
                return getNewLatestEntryFuture(tenantId, entityId, query);
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), isRemoved));
        }, MoreExecutors.directExecutor());
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
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        ListenableFuture<ReadTsKvQueryResult> future = aggregationTimeseriesDao.findAllAsync(tenantId, entityId, findNewLatestQuery);

        return Futures.transformAsync(future, result -> {
            var entryList = result.getData();
            if (entryList.size() == 1) {
                TsKvEntry entry = entryList.get(0);
                return Futures.transform(saveLatest(tenantId, entityId, entryList.get(0)), v -> new TsKvLatestRemovingResult(entry), MoreExecutors.directExecutor());
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), true));
        }, readResultsProcessingExecutor);
    }

    /**
     * 功能：删除或清理`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> deleteLatest(TenantId tenantId, EntityId entityId, String key) {
        Statement delete = QueryBuilder.deleteFrom(ModelConstants.TS_KV_LATEST_CF)
                .whereColumn(ModelConstants.ENTITY_TYPE_COLUMN).isEqualTo(literal(entityId.getEntityType().name()))
                .whereColumn(ModelConstants.ENTITY_ID_COLUMN).isEqualTo(literal(entityId.getId()))
                .whereColumn(ModelConstants.KEY_COLUMN).isEqualTo(literal(key)).build();
        log.debug("Remove request: {}", delete.toString());
        return getFuture(executeAsyncWrite(tenantId, delete), rs -> null);
    }

    /**
     * 功能：转换时间戳。
     * 参数：
     * - `rs`：`rs` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<TsKvEntry>> convertAsyncResultSetToTsKvEntryList(TbResultSet rs) {
        return Futures.transform(rs.allRows(readResultsProcessingExecutor),
                rows -> this.convertResultToTsKvEntryList(rows), readResultsProcessingExecutor);
    }

    /**
     * 功能：获取`Latest Stmt`。
     * 参数：无。
     * 返回：处理结果。
     */
    private PreparedStatement getLatestStmt() {
        if (latestInsertStmt == null) {
            latestInsertStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_LATEST_CF +
                    "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                    "," + ModelConstants.ENTITY_ID_COLUMN +
                    "," + ModelConstants.KEY_COLUMN +
                    "," + ModelConstants.TS_COLUMN +
                    "," + ModelConstants.BOOLEAN_VALUE_COLUMN +
                    "," + ModelConstants.STRING_VALUE_COLUMN +
                    "," + ModelConstants.LONG_VALUE_COLUMN +
                    "," + ModelConstants.DOUBLE_VALUE_COLUMN +
                    "," + ModelConstants.JSON_VALUE_COLUMN + ")" +
                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
        }
        return latestInsertStmt;
    }

    /**
     * 功能：获取`Find Latest Stmt`。
     * 参数：无。
     * 返回：处理结果。
     */
    private PreparedStatement getFindLatestStmt() {
        if (findLatestStmt == null) {
            findLatestStmt = prepare(SELECT_PREFIX +
                    ModelConstants.KEY_COLUMN + "," +
                    ModelConstants.TS_COLUMN + "," +
                    ModelConstants.STRING_VALUE_COLUMN + "," +
                    ModelConstants.BOOLEAN_VALUE_COLUMN + "," +
                    ModelConstants.LONG_VALUE_COLUMN + "," +
                    ModelConstants.DOUBLE_VALUE_COLUMN + "," +
                    ModelConstants.JSON_VALUE_COLUMN + " " +
                    "FROM " + ModelConstants.TS_KV_LATEST_CF + " " +
                    "WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM +
                    "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM +
                    "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM);
        }
        return findLatestStmt;
    }

    /**
     * 功能：获取`Find All Latest Stmt`。
     * 参数：无。
     * 返回：处理结果。
     */
    private PreparedStatement getFindAllLatestStmt() {
        if (findAllLatestStmt == null) {
            findAllLatestStmt = prepare(SELECT_PREFIX +
                    ModelConstants.KEY_COLUMN + "," +
                    ModelConstants.TS_COLUMN + "," +
                    ModelConstants.STRING_VALUE_COLUMN + "," +
                    ModelConstants.BOOLEAN_VALUE_COLUMN + "," +
                    ModelConstants.LONG_VALUE_COLUMN + "," +
                    ModelConstants.DOUBLE_VALUE_COLUMN + "," +
                    ModelConstants.JSON_VALUE_COLUMN + " " +
                    "FROM " + ModelConstants.TS_KV_LATEST_CF + " " +
                    "WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM +
                    "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM);
        }
        return findAllLatestStmt;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraBaseTimeseriesLatestDao` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */

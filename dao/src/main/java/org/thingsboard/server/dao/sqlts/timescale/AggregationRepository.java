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
package org.thingsboard.server.dao.sqlts.timescale;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsOrTsLatestDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`AggregationRepository` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@Repository
@TimescaleDBTsOrTsLatestDao
public class AggregationRepository {

    /**
     * `FIND_AVG`常量，用于统一引用固定值。
     */
    public static final String FIND_AVG = "findAvg";
    public static final String FIND_MAX = "findMax";
    /**
     * `FIND_MIN`常量，用于统一引用固定值。
     */
    public static final String FIND_MIN = "findMin";
    public static final String FIND_SUM = "findSum";
    /**
     * 数量常量，用于统一引用固定值。
     */
    public static final String FIND_COUNT = "findCount";

    public static final String FROM_WHERE_CLAUSE = "FROM ts_kv tskv WHERE " +
            "tskv.entity_id = cast(:entityId AS uuid) " +
            "AND tskv.key= cast(:entityKey AS int) " +
            "AND tskv.ts >= :startTs AND tskv.ts < :endTs " +
            "GROUP BY tskv.entity_id, tskv.key, tsBucket " +
            "ORDER BY tskv.entity_id, tskv.key, tsBucket";

    public static final String FIND_AVG_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(COALESCE(tskv.long_v, 0)) AS longValue, " +
            "SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "null AS strValue, 'AVG' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_MAX_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "MAX(COALESCE(tskv.long_v, -9223372036854775807)) AS longValue, " +
            "MAX(COALESCE(tskv.dbl_v, -1.79769E+308)) as doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "MAX(tskv.str_v) AS strValue, 'MAX' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_MIN_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "MIN(COALESCE(tskv.long_v, 9223372036854775807)) AS longValue, " +
            "MIN(COALESCE(tskv.dbl_v, 1.79769E+308)) as doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "MIN(tskv.str_v) AS strValue, 'MIN' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_SUM_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(COALESCE(tskv.long_v, 0)) AS longValue, SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "null AS strValue, null AS jsonValue, 'SUM' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_COUNT_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(CASE WHEN tskv.bool_v IS NULL THEN 0 ELSE 1 END) AS booleanValueCount, " +
            "SUM(CASE WHEN tskv.str_v IS NULL THEN 0 ELSE 1 END) AS strValueCount, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longValueCount, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleValueCount, " +
            "SUM(CASE WHEN tskv.json_v IS NULL THEN 0 ELSE 1 END) AS jsonValueCount, " +
            "MAX(tskv.ts) AS maxAggTs ";

    /**
     * 实体，负责处理对应任务或消息。
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 功能：获取`Avg`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findAvg(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_AVG);
    }

    /**
     * 功能：获取`Max`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findMax(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MAX);
    }

    /**
     * 功能：获取`Min`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findMin(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MIN);
    }

    /**
     * 功能：获取`Sum`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findSum(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_SUM);
    }

    /**
     * 功能：获取数量。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findCount(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_COUNT);
    }

    /**
     * 功能：获取`Result List`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private List getResultList(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs, String query) {
        return entityManager.createNamedQuery(query)
                .setParameter("entityId", entityId)
                .setParameter("entityKey", entityKey)
                .setParameter("timeBucket", timeBucket)
                .setParameter("startTs", startTs)
                .setParameter("endTs", endTs)
                .getResultList();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AggregationRepository` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */

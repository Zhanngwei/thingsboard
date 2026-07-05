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
package org.thingsboard.server.dao.sqlts.ts;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`TsKvRepository` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
public interface TsKvRepository extends JpaRepository<TsKvEntity, TsKvCompositeKey> {

    /*
    * Using native query to avoid adding 'nulls first' or 'nulls last' (ignoring spring.jpa.properties.hibernate.order_by.default_null_ordering)
    * to the order so that index scan is done instead of full scan.
    *
    * Note: even when setting custom NullHandling for the Sort.Order for non-native queries,
    * it will be ignored and default_null_ordering will be used
    * */
    /**
     * 功能：获取数量限制。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query(value = "SELECT * FROM ts_kv WHERE entity_id = :entityId " +
            "AND key = :entityKey AND ts >= :startTs AND ts < :endTs ", nativeQuery = true)
    List<TsKvEntity> findAllWithLimit(@Param("entityId") UUID entityId,
                                      @Param("entityKey") int key,
                                      @Param("startTs") long startTs,
                                      @Param("endTs") long endTs,
                                      Pageable pageable);

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM TsKvEntity tskv WHERE tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    void delete(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`String Max`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                @Param("entityKey") int key,
                @Param("startTs") long startTs,
                @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MAX(tskv.strValue), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.strValue IS NOT NULL " +
            "AND tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findStringMax(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Numeric Max`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                                @Param("entityKey") int entityKey,
                                                @Param("startTs") long startTs,
                                                @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MAX(COALESCE(tskv.longValue, -9223372036854775807)), " +
            "MAX(COALESCE(tskv.doubleValue, -1.79769E+308)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'MAX', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findNumericMax(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`String Min`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);


    @Query("SELECT new TsKvEntity(MIN(tskv.strValue), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.strValue IS NOT NULL " +
            "AND tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findStringMin(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Numeric Min`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MIN(COALESCE(tskv.longValue, 9223372036854775807)), " +
            "MIN(COALESCE(tskv.doubleValue, 1.79769E+308)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'MIN', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findNumericMin(
    /**
     * 功能：获取数量。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityId") UUID entityId,
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(CASE WHEN tskv.booleanValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.strValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.jsonValue IS NULL THEN 0 ELSE 1 END), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findCount(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Avg`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                            @Param("entityKey") int entityKey,
                                            @Param("startTs") long startTs,
                                            @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(COALESCE(tskv.longValue, 0)), " +
            "SUM(COALESCE(tskv.doubleValue, 0.0)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'AVG', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findAvg(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Sum`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(COALESCE(tskv.longValue, 0)), " +
            "SUM(COALESCE(tskv.doubleValue, 0.0)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'SUM', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findSum(@Param("entityId") UUID entityId,
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

}

/*
 * 本类总结：
 * 1. 核心职责：`TsKvRepository` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
